# 🧪 个人博客后端测试与开发流程指南

本文档详细介绍了在 Spring Boot 项目中如何进行规范的测试，以及推荐的开发流程。

---

## 1. Spring Boot 测试工具箱 🧰

本项目基于 `spring-boot-starter-test`，已经内置了业界最主流的测试工具：

| 工具 | 用途 | 典型场景 |
| :--- | :--- | :--- |
| **JUnit 5** | 单元测试框架 | 编写测试用例的基础 (@Test, @DisplayName) |
| **Mockito** | 模拟框架 | 模拟 Service 或 Repository 的行为，隔离依赖 |
| **AssertJ** | 断言库 | 编写流畅、可读性高的断言 (assertThat...) |
| **Spring Test** | 集成测试支持 | 加载 Spring 上下文，测试 Bean 的集成 |
| **ArchUnit** | 架构测试 | 确保代码遵循架构规范 (如 Controller 不能直接调 Mapper) |
| **Testcontainers** | 容器化测试 | (可选) 在 Docker 中启动真实的 MySQL/Redis 进行测试 |

---

## 2. 测试金字塔策略 📐

我们遵循“测试金字塔”原则，不同层级的测试关注点不同：

### 2.1 单元测试 (Unit Tests) - 占比 70%
*   **目标**: 测试单个类（通常是 Service）的业务逻辑。
*   **特点**: **不启动 Spring 上下文**，速度极快（毫秒级）。
*   **工具**: JUnit 5 + Mockito。
*   **示例**:
    ```java
    @ExtendWith(MockitoExtension.class) // 1. 启用 Mockito
    class UserServiceImplTest {

        @Mock
        private UserMapper userMapper; // 2. 模拟依赖

        @InjectMocks
        private UserServiceImpl userService; // 3. 注入模拟对象

        @Test
        void should_register_user_successfully() {
            // Given
            UserDTO dto = new UserDTO("test", "123456");
            given(userMapper.selectByUsername("test")).willReturn(null); // 模拟数据库没查到人

            // When
            userService.register(dto);

            // Then
            verify(userMapper).insert(any(User.class)); // 验证是否调用了插入方法
        }
    }
    ```

### 2.2 集成测试 (Integration Tests) - 占比 20%
*   **目标**: 测试多个组件的协作（如 Controller + Service + Database）。
*   **特点**: **启动 Spring 上下文**，速度较慢。
*   **工具**: `@SpringBootTest`, `@AutoConfigureMockMvc`。
*   **示例**:
    ```java
    @SpringBootTest
    @AutoConfigureMockMvc
    class UserControllerTest {

        @Autowired
        private MockMvc mockMvc; // 模拟 HTTP 请求

        @Test
        void should_return_200_when_login_success() throws Exception {
            mockMvc.perform(post("/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").exists());
        }
    }
    ```

### 2.3 架构测试 (Architecture Tests) - 占比 10%
*   **目标**: 自动守护架构红线（如“Service 只能依赖 API”）。
*   **工具**: ArchUnit。
*   **位置**: `blog-application/src/test/java/.../ArchitectureTest.java`。

---

## 3. 规范的开发流程 (Workflow) 🔄

一个高质量功能的开发应该遵循 **TDD (测试驱动开发)** 或 **测试先行** 的思想：

1.  **设计 (Design)**
    *   在 `*-api` 模块定义 DTO 和 Interface。
    *   在数据库设计表结构。

2.  **编写测试 (Write Tests)**
    *   *进阶做法*: 先写一个失败的单元测试（红）。
    *   *普通做法*: 脑子里构思好测试场景（正常流程、异常流程、边界条件）。

3.  **编码 (Coding)**
    *   实现 Service 业务逻辑。
    *   实现 Controller 接口。

4.  **验证 (Verify)**
    *   运行单元测试，确保逻辑覆盖率。
    *   运行集成测试，确保接口连通性。
    *   (可选) 使用 Postman / Swagger UI 进行手动冒烟测试。

5.  **重构 (Refactor)**
    *   在测试保护下优化代码结构。

---

## 4. 常见问题 (FAQ)

**Q: 每次都要启动数据库吗？**
A: 单元测试不需要（用 Mockito）。集成测试建议使用 H2 内存数据库或 Testcontainers 启动临时 Docker 数据库，保持环境纯净。

**Q: Controller 层需要测什么？**
A: 重点测试参数校验 (`@Valid`)、HTTP 状态码映射、全局异常处理是否生效。业务逻辑应下沉到 Service 层测试。

**Q: 如何查看测试覆盖率？**
A: 在 IDEA 中，右键点击测试文件夹 -> "Run 'Tests' with Coverage"。

---
*文档维护者: Antigravity Agent*
*最后更新: 2025-12-04*
