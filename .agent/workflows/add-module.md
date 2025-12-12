---
description: 如何添加新的业务模块（如 blog-module-xxx）
---

# 添加新业务模块

## 前置条件
- 确定模块名称，如 `xxx`（小写，如 article, comment, file）
- 确定表前缀，如 `xxx_`

## 操作步骤

### 1. 创建模块目录结构
在 `blog-modules/` 下创建：
```
blog-module-xxx/
├── pom.xml
├── blog-xxx-api/
│   ├── pom.xml
│   └── src/main/java/com/blog/xxx/api/
│       ├── dto/
│       └── vo/
└── blog-xxx-service/
    ├── pom.xml
    └── src/main/java/com/blog/xxx/
        ├── controller/
        ├── service/
        ├── entity/
        ├── mapper/
        └── converter/
```

// turbo
### 2. 创建父模块 pom.xml
```xml
<project>
    <parent>
        <groupId>com.blog</groupId>
        <artifactId>blog-modules</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>blog-module-xxx</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>blog-xxx-api</module>
        <module>blog-xxx-service</module>
    </modules>
</project>
```

### 3. 更新根 pom.xml
在 `<dependencyManagement>` 中添加：
```xml
<dependency>
    <groupId>com.blog</groupId>
    <artifactId>blog-xxx-api</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.blog</groupId>
    <artifactId>blog-xxx-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 4. 更新 blog-modules/pom.xml
在 `<modules>` 中添加：
```xml
<module>blog-module-xxx</module>
```

### 5. 更新 blog-application/pom.xml
在 `<dependencies>` 中添加：
```xml
<dependency>
    <groupId>com.blog</groupId>
    <artifactId>blog-xxx-service</artifactId>
</dependency>
```

// turbo
### 6. 验证构建
```bash
mvn clean install -DskipTests
```

## 检查清单
- [ ] 模块目录结构正确
- [ ] 所有 pom.xml 已创建
- [ ] 根 pom.xml 已更新
- [ ] blog-application 已添加依赖
- [ ] 构建成功
