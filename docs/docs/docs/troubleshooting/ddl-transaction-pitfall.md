## 🤖 Assistant


---

### 问题复盘与排查思路总结

#### **问题现象**

在使用 MyBatis-Plus DDL-Auto 插件进行数据库 schema 自动初始化时，出现了 `ddl_history` 表被成功创建，但其中没有任何脚本执行记录的异常情况。这意味着初始化流程被部分执行，但记录历史的 `INSERT` 操作最终失败。

---

#### **第一阶段：初步诊断与首次尝试**

1. **问题表现**：`ddl_history` 表为空。
2. **核心猜想**：`CREATE TABLE` 操作通常会隐式提交（Implicit Commit），而 `INSERT` 操作则需要显式提交。如果二者在一个事务中，而该事务最后没有被 `commit` 而是被 `rollback`，就会导致表已创建但数据插入失败的现象。
3. **日志分析 (初步)**：在最初的日志中，我们发现了关键线索：
 ```log
 com.zaxxer.hikari.pool.ProxyConnection: ... Executed rollback on connection ... due to dirty commit state on close().
 ```
这条日志是“确凿证据”，证实了 HikariCP 连接池在回收连接时，发现了一个未提交的事务（即“脏连接”），并为了安全起见自动执行了回滚。
4. **定位原因**：MyBatis-Plus 的 `DdlApplicationRunner` 在执行过程中，通过 `runner.setAutoCommit(false)` 开启了手动事务模式。但由于它只是一个普通的 `ApplicationRunner`，在其 `run` 方法执行完毕后，并没有任何机制来显式 `commit` 这个事务。
5. **首次解决方案尝试**：
* **思路**：利用 Spring 强大的声明式事务管理。创建一个新的、带有 `@Transactional` 注解的 `ApplicationRunner`（我们称之为 `TransactionalDdlRunner`），在这个 Runner 内部去调用原始的 `DdlApplicationRunner`。
* **预期**：Spring AOP 会为 `TransactionalDdlRunner.run()` 方法创建一个事务。当方法正常结束时，Spring 会自动提交该事务，从而持久化 `INSERT` 操作。

---

#### **第二阶段：深入分析与最终解决方案**

1. **新问题出现**：应用首次解决方案后，`ddl_history` 表依然为空。问题并未解决。
2. **日志分析 (深度)**：仔细审查新生成的日志，发现了更深层次的问题：**DDL 逻辑被执行了两次**。
* **第一次执行（无事务）**：日志显示，一个 `DdlApplicationRunner` 在没有任何事务包裹的情况下被执行了。这次执行创建了表，但 `INSERT` 被回滚。
* **第二次执行（有事务）**：紧接着，我们创建的 `TransactionalDdlRunner` 被执行，并正确地开启了事务。但在其内部调用 DDL 逻辑时，MyBatis-Plus 检测到 `ddl_history` 表已存在，且可能内部有机制防止重复执行脚本，因此跳过了所有 SQL 执行，包括 `INSERT`。最终，一个空的事务被提交。
3. **核心猜想：为什么会执行两次？**
* 通过分析 Spring Boot 的启动流程，我们意识到 `ApplicationRunner` 是一个特殊的接口。Spring 容器在启动阶段会自动扫描并执行所有被注册为 Bean 且实现了此接口的类。
* 在我们之前的配置中，存在**两个**这样的 Bean：
1. `DdlApplicationRunner`：由 `DdlExecutionConfig` 类通过 `@Bean` 方法创建。
2. `TransactionalDdlRunner`：通过 `@Component` 注解被扫描和注册。
* 这就导致 Spring Boot 按照一定的（不确定的）顺序，依次执行了这两个 Runner，从而引发了上述的“二次执行”问题。
4. **最终解决方案：整合与单一化**
* **思路**：必须确保 DDL 的配置和执行逻辑在一个统一的、具备事务能力的单元中完成，并且这个单元作为**唯一**的 `ApplicationRunner` Bean 被 Spring 管理。
* **实施步骤**：
1. **废弃分离的配置**：删除原有的 `DdlExecutionConfig` 和 `TransactionalDdlRunner`。
2. **创建统一的初始化器**：新建一个 `DdlInitializer` 类。
3. **角色合一**：让 `DdlInitializer` 同时承担多个角色：
* 它是一个 `@Configuration` Bean，能被 Spring 扫描到。
* 它实现了 `ApplicationRunner` 接口，所以 Spring 会在启动时调用它的 `run` 方法。
* 它的 `run` 方法被 `@Transactional` 注解，确保了执行逻辑的原子性。
4. **控制权反转**：在 `DdlInitializer` 的 `run` 方法内部，`@Autowired` 注入所有 `IDdl` 的实现。**关键在于**，将 `DdlApplicationRunner` 作为一个**局部变量**在方法内 `new` 出来，而不是作为 Spring Bean。这样它就只是一个普通的执行工具，Spring 不会再自动调用它的 `run` 方法。
5. **完整配置**：在这个事务方法内，完成对 `DdlApplicationRunner` 实例的所有配置（如 `setAutoCommit`, 错误处理器等），然后调用其 `run` 方法。
5. **结果**：现在整个 DDL 初始化流程只被调用一次，并且这次调用从始至终都处于一个被 Spring 正确管理的事务中。当 `DdlInitializer.run()` 方法成功执行完毕后，事务被提交，`INSERT` 语句被成功持久化到数据库。

---

### **经验总结 (Key Takeaways)**

1. **警惕框架的自动化机制**：Spring Boot 的 `ApplicationRunner` 自动执行机制非常方便，但也可能导致意想不到的重复执行，尤其是在通过 `@Bean` 和 `@Component` 等多种方式注册了实现了相同接口的Bean时。
2. **日志是最终的真相来源**：当行为与预期不符时，详细的 DEBUG 级别日志是揭示问题的最可靠工具。通过日志，我们能清晰地看到方法的调用顺序、事务的创建与提交/回滚、以及框架的内部决策过程。
3. **事务边界至关重要**：对于包含多个数据库写操作的业务逻辑，必须确保它们被包裹在同一个事务边界内。Spring 的 `@Transactional` 是实现这一点的首选方式，但要确保它被正确地应用在执行入口上。
4. **区分“Bean”与“普通对象”**：在 Spring 环境中，要清晰地认识到一个类是被当作 Spring Bean 管理，还是仅仅作为一个普通的 Java 对象被创建和使用。这个区别决定了它是否会参与到 Spring 的生命周期管理（如自动执行 `ApplicationRunner`）中。
5. **优雅的解决方案往往是整合与简化**：当遇到由多个组件交互引起的复杂问题时，通常最佳的解决方案是重新设计，将分散的职责整合到一个更内聚、更清晰的单一组件中，而不是在现有复杂结构上“打补丁”。
6. 原始修改文件
   [DdlExecutionConfig.java](../../static/code-samples/DdlExecutionConfig.java)
   [TransactionalDdlRunner.java](../../static/code-samples/TransactionalDdlRunner.java)