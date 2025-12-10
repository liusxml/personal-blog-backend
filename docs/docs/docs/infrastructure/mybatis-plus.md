---
sidebar_position: 4
---

# MyBatis-Plus 使用指南

Personal Blog Backend 使用 **MyBatis-Plus 3.5.14** 作为 ORM 框架，简化数据库操作并提供丰富的增强功能。

## 🎯 简介

MyBatis-Plus (opens in a new tab) 是一个 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变，为简化开发、提高效率而生。

**核心特性**:
- ✅ **无侵入** - 只做增强不做改变，引入它不会对现有工程产生影响
- ✅ **损耗小** - 启动即会自动注入基本 CRUD，性能基本无损耗
- ✅ **强大的 CRUD 操作** - 内置通用 Mapper、Service，仅需少量配置即可实现单表大部分 CRUD
- ✅ **支持 Lambda 形式调用** - 通过 Lambda 表达式，方便编写查询条件
- ✅ **支持主键自动生成** - 支持 4 种主键策略（含分布式唯一 ID），可自由配置，完美解决主键问题
- ✅ **支持 ActiveRecord 模式** - 支持 ActiveRecord 形式调用，实体类只需继承 Model 类即可
- ✅ **支持自定义全局通用操作** - 支持全局通用方法注入（Write once, use anywhere）
- ✅ **内置分页插件** - 基于 MyBatis 物理分页，开发者无需关心具体操作，配置好插件之后，写分页等同于普通 List 查询
- ✅ **内置性能分析插件** - 可输出 SQL 语句以及其执行时间，建议开发测试时启用该功能
- ✅ **内置全局拦截插件** - 提供全表 delete、update 操作智能分析阻断，也可自定义拦截规则，预防误操作

## 🔧 项目配置

### 1. 依赖引入

```xml title="blog-common/pom.xml"
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.14</version>
</dependency>
```

### 2. application.yaml 配置

```yaml title="blog-application/src/main/resources/application.yaml"
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml # Mapper XML 扫描路径
  type-aliases-package: com.blog.**.entity      # 实体别名包
  
  global-config:
    banner: false                               # 关闭启动 Banner
    db-config:
      id-type: assign_uuid                      # 主键策略（雪花算法）
      logic-delete-field: isDeleted             # 逻辑删除字段
      logic-delete-value: 1                     # 删除值
      logic-not-delete-value: 0                 # 未删除值
      
  configuration:
    map-underscore-to-camel-case: true          # 开启驼峰命名转换
    call-setters-on-nulls: true                 # NULL值也调用Setter
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl # SQL日志实现
```

### 3. 插件配置

项目配置了多个 MyBatis-Plus 插件以增强功能和安全性：

```java title="blog-application/src/main/java/com/blog/config/MybatisPlusConfig.java"
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 1. 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        // 2. 分页插件（指定MySQL数据库类型）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 3. 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}
```

**插件说明**:
- **BlockAttackInnerInterceptor**: 防止全表更新和删除操作，避免误操作
- **PaginationInnerInterceptor**: 物理分页插件，自动处理分页逻辑
- **OptimisticLockerInnerInterceptor**: 乐观锁插件，支持 `@Version` 注解

### 4. 自动填充配置

项目配置了字段自动填充处理器，自动填充创建时间和更新时间：

```java title="blog-application/src/main/java/com/blog/handler/MyMetaObjectHandler.java"
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill ....");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
```

**注册配置**:
```java title="blog-application/src/main/java/com/blog/config/MybatisPlusHandlerConfig.java"
@Configuration
public class MybatisPlusHandlerConfig {
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }
    
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            // 关闭 Banner
            properties.getGlobalConfig().setBanner(false);
            
            // 配置默认枚举处理器
            MybatisPlusProperties.CoreConfiguration configuration = properties.getConfiguration();
            if (configuration == null) {
                configuration = new MybatisPlusProperties.CoreConfiguration();
                properties.setConfiguration(configuration);
            }
            configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);
        };
    }
}
```

## 📝 实体类定义

### 1. 基本注解

```java title="blog-system-service/src/main/java/com/blog/system/entity/SysUser.java"
@Data
@TableName("sys_user")  // 指定表名
public class SysUser {
    
    /**
     * 主键 - 使用雪花算法自动生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 账户状态 (1-正常, 0-禁用)
     */
    private Integer status;
    
    /**
     * 版本号 - 乐观锁字段
     */
    @Version
    private Integer version;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间 - 自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标记 (0-未删, 1-已删)
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 备注
     */
    private String remark;
}
```

### 2. 常用注解说明

| 注解 | 作用 | 示例 |
|------|------|------|
| `@TableName` | 指定表名 | `@TableName("sys_user")` |
| `@TableId` | 指定主键及策略 | `@TableId(type = IdType.ASSIGN_ID)` |
| `@TableField` | 指定字段属性 | `@TableField(fill = FieldFill.INSERT)` |
| `@Version` | 乐观锁字段 | `@Version` |
| `@TableLogic` | 逻辑删除字段 | `@TableLogic` |

**主键策略** (`IdType`):
- `ASSIGN_ID`: 雪花算法生成 Long 类型 ID（项目默认）
- `ASSIGN_UUID`: UUID 生成
- `AUTO`: 数据库自增
- `INPUT`: 手动输入

## 🔨 Mapper 层开发

### 1. 基础 Mapper 定义

```java title="blog-system-service/src/main/java/com/blog/system/mapper/UserMapper.java"
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    
    /**
     * 根据用户ID查询用户的角色列表（自定义方法）
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);
    
    /**
     * 批量查询用户
     */
    List<SysUser> selectByIds(@Param("ids") List<Long> ids);
    
    /**
     * 检查用户名是否存在
     */
    Boolean existsByUsername(@Param("username") String username);
}
```

### 2. BaseMapper 内置方法

继承 `BaseMapper<T>` 后自动拥有以下方法：

**插入**:
```java
int insert(T entity);                    // 插入一条记录
```

**删除**:
```java
int deleteById(Serializable id);         // 根据ID删除
int deleteByMap(Map<String, Object> columnMap); // 根据条件删除
int delete(Wrapper<T> wrapper);          // 根据Wrapper删除
int deleteBatchIds(Collection<? extends Serializable> idList); // 批量删除
```

**更新**:
```java
int updateById(T entity);                // 根据ID更新
int update(T entity, Wrapper<T> updateWrapper); // 根据Wrapper更新
```

**查询**:
```java
T selectById(Serializable id);           // 根据ID查询
List<T> selectBatchIds(Collection<? extends Serializable> idList); // 批量查询
T selectOne(Wrapper<T> queryWrapper);    // 查询一条记录
Long selectCount(Wrapper<T> queryWrapper); // 查询总记录数
List<T> selectList(Wrapper<T> queryWrapper); // 查询列表
List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper); // 查询列表（Map）
Page<T> selectPage(Page<T> page, Wrapper<T> queryWrapper); // 分页查询
```

## 💼 Service 层开发

### 1. 使用 IService 接口

```java
public interface IUserService extends IService<SysUser> {
    // 自定义业务方法
    SysUser getUserByUsername(String username);
}
```

### 2. 实现类

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> 
        implements IUserService {
    
    @Override
    public SysUser getUserByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }
}
```

### 3. IService 常用方法

**保存**:
```java
boolean save(T entity);                  // 插入一条记录
boolean saveBatch(Collection<T> entityList); // 批量插入
boolean saveOrUpdate(T entity);          // 存在则更新，否则插入
```

**删除**:
```java
boolean removeById(Serializable id);     // 根据ID删除
boolean removeByIds(Collection<? extends Serializable> idList); // 批量删除
boolean remove(Wrapper<T> queryWrapper); // 根据条件删除
```

**更新**:
```java
boolean updateById(T entity);            // 根据ID更新
boolean updateBatchById(Collection<T> entityList); // 批量更新
boolean update(Wrapper<T> updateWrapper); // 根据条件更新
```

**查询**:
```java
T getById(Serializable id);              // 根据ID查询
List<T> listByIds(Collection<? extends Serializable> idList); // 批量查询
List<T> list(Wrapper<T> queryWrapper);   // 查询列表
Page<T> page(Page<T> page, Wrapper<T> queryWrapper); // 分页查询
long count(Wrapper<T> queryWrapper);     // 查询总数
```

## 🔍 条件构造器

### 1. QueryWrapper 示例

```java
// 查询用户名为"admin"且状态为1的用户
QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("username", "admin")
            .eq("status", 1);
List<SysUser> users = userMapper.selectList(queryWrapper);

// 模糊查询
QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
wrapper.like("username", "test")
       .or()
       .like("email", "test");
List<SysUser> list = userMapper.selectList(wrapper);
```

### 2. LambdaQueryWrapper 示例（推荐）

```java
// Lambda方式 - 类型安全，避免字符串硬编码
LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SysUser::getUsername, "admin")
       .eq(SysUser::getStatus, 1);
List<SysUser> users = userMapper.selectList(wrapper);

// 动态条件
LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
wrapper.like(StringUtils.isNotBlank(username), SysUser::getUsername, username)
       .eq(status != null, SysUser::getStatus, status)
       .ge(startTime != null, SysUser::getCreateTime, startTime);
```

### 3. UpdateWrapper 示例

```java
// 更新用户状态
UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
updateWrapper.eq("id", userId)
             .set("status", 0);
userMapper.update(null, updateWrapper);

// Lambda方式
LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(SysUser::getId, userId)
       .set(SysUser::getStatus, 0);
userMapper.update(null, wrapper);
```

## 📄 分页查询

### 1. 基本分页

```java
// 创建分页对象
Page<SysUser> page = new Page<>(1, 10); // 第1页，每页10条

// 执行分页查询
LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SysUser::getStatus, 1);
Page<SysUser> resultPage = userMapper.selectPage(page, wrapper);

// 获取分页结果
List<SysUser> records = resultPage.getRecords(); // 数据列表
long total = resultPage.getTotal();               // 总记录数
long pages = resultPage.getPages();               // 总页数
```

### 2. Service层分页

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> 
        implements IUserService {
    
    public Page<SysUser> getUserPage(int current, int size, String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), SysUser::getUsername, keyword)
               .eq(SysUser::getStatus, 1)
               .orderByDesc(SysUser::getCreateTime);
        
        return this.page(page, wrapper);
    }
}
```

## ⚠️ 最佳实践

### 1. 优先使用 Lambda 方式

```java
// ✅ 推荐：使用 Lambda，类型安全
LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SysUser::getUsername, "admin");

// ❌ 不推荐：字符串硬编码，容易拼写错误
QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
wrapper.eq("username", "admin");
```

### 2. 合理使用逻辑删除

```java
// 配置了 @TableLogic 后，以下操作自动变为逻辑删除
userMapper.deleteById(1L);  // UPDATE sys_user SET is_deleted=1 WHERE id=1

// 查询时自动过滤已删除数据
userMapper.selectList(null); // SELECT * FROM sys_user WHERE is_deleted=0
```

### 3. 利用乐观锁防止并发问题

```java
// 实体类中添加 @Version 注解
@Version
private Integer version;

// 更新时会自动比对版本号
SysUser user = userMapper.selectById(1L);  // version=1
user.setUsername("newName");
userMapper.updateById(user);  
// UPDATE sys_user SET username='newName', version=2 WHERE id=1 AND version=1
```

### 4. 防止全表更新和删除

```java
// 配置了 BlockAttackInnerInterceptor 后，以下操作会被拦截
userMapper.delete(null);  // ❌ 抛出异常，禁止全表删除
userMapper.update(entity, null); // ❌ 抛出异常，禁止全表更新

// 必须带条件
LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SysUser::getStatus, 0);
user Mapper.delete(wrapper);  // ✅ 允许
```

### 5. 自动填充字段

```java
// 实体类配置自动填充
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;

// 插入和更新时自动填充，无需手动设置
SysUser user = new SysUser();
user.setUsername("test");
userMapper.insert(user);  // createTime 和 updateTime 自动填充
```

## 📚 参考资源

- **官方文档**: https://baomidou.com
- **GitHub**: https://github.com/baomidou/mybatis-plus
- **代码生成器**: https://baomidou.com/pages/779a6e/

---

**提示**: MyBatis-Plus 只做增强不做改变，可以无缝集成到现有项目中。
