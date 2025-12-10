---
sidebar_position: 1
---

# Security 安全指南

Personal Blog Backend 使用 **Spring Security + JWT** 实现无状态的认证和授权机制。本文档介绍安全配置和最佳实践。

## 🎯 安全架构概览

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant Filter as JWT过滤器
    
    Client->>Filter: 1. 发送请求(带Token)
    Filter->>Filter: 2. 提取JWT Token
    Filter->>Filter: 3. 验证Token签名
    Filter->>Filter: 4. 从Token解析用户信息<br/>(username, userId, roles)
    Filter->>Filter: 5. 构建权限列表
    Filter->>Filter: 6. 创建认证对象
    Filter->>Filter: 7. 设置Security Context
    Filter-->>Client: 8. 继续请求处理
```

:::tip JWT 无状态认证
所有用户信息（用户名、ID、角色）都存储在 JWT Token 中，**无需查询数据库**，实现真正的无状态认证，性能优秀。
:::

## 🔒 核心组件

### 1. Security 配置类（三链架构）⭐

项目采用**多过滤链设计模式**，使用 `@Order` 实现优先级控制，支持单体到微服务的平滑演进。

```java title="blog-application/src/main/java/com/blog/config/SecurityConfig.java"
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级安全注解（@PreAuthorize等）
public class SecurityConfig {
    
    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(SecurityProperties securityProperties,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.securityProperties = securityProperties;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    // ============================================
    // 过滤链 1：白名单链 (@Order(1)) - 最高优先级
    // ============================================
    
    /**
     * 白名单路径安全过滤链
     * 
     * 职责：对配置的 permitAllUrls 路径执行 permitAll()
     * 包括：/actuator/health, /actuator/info, /v3/api-docs/** 等
     */
    @Bean
    @Order(1)
    public SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
        List<String> urls = getSafePermitAllUrls();
        if (CollectionUtils.isEmpty(urls)) {
            http.securityMatcher(r -> false);  // 空白名单时不匹配任何请求
            return http.build();
        }
        
        http.securityMatcher(urls.toArray(new String[0]))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> 
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
    
    // ============================================
    // 过滤链 2：JWT 认证链 (@Order(2)) - API 访问控制
    // ============================================
    
    /**
     * JWT 认证过滤链
     * 
     * 匹配：/auth/** 和 /api/** 的请求
     * 认证方式：JWT Bearer Token
     */
    @Bean
    @Order(2)
    public SecurityFilterChain jwtChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/auth/**", "/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> 
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 注册和登录公开
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                // 其他 /api/** 需要认证
                .anyRequest().authenticated()
            )
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    // ============================================
    // 过滤链 3：默认认证链 (@Order(3)) - 兜底策略
    // ============================================
    
    /**
     * 默认认证过滤链
     * 
     * 匹配：所有未被白名单和JWT链处理的请求
     * 认证方式：HTTP Basic + Form Login
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> 
                s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> 
                auth.anyRequest().authenticated())
            .formLogin(form -> 
                form.defaultSuccessUrl("/swagger-ui.html", true))
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 私有工具方法
    private List<String> getSafePermitAllUrls() {
        List<String> rawUrls = securityProperties.getPermitAllUrls();
        if (CollectionUtils.isEmpty(rawUrls)) {
            return ImmutableList.of();
        }
        return rawUrls.stream()
            .filter(Objects::nonNull)
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::trim)
            .map(this::ensureLeadingSlash)
            .collect(ImmutableList.toImmutableList());
    }
    
    private String ensureLeadingSlash(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}
```

:::tip 三链架构优势
- ✅ **关注点分离** - 每条链职责单一，易于理解和维护
- ✅ **优先级清晰** - `@Order` 控制执行顺序，避免配置冲突
- ✅ **易于扩展** - 添加新认证方式无需修改现有链
- ✅ **微服务就绪** - 拆分时每个服务可独立配置过滤链
:::

### 2. SecurityProperties 配置类

使用 `@ConfigurationProperties` 模式管理安全配置，提供类型安全和IDE支持。

```java title="blog-common/src/main/java/com/blog/common/config/SecurityProperties.java"
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    /**
     * URL 白名单配置
     */
    private List<String> permitAllUrls = new ArrayList<>();
    
    /**
     * JWT 签名密钥（至少256位）
     */
    private String jwtSecret = "default-secret-key-change-in-production-at-least-256-bits-long";
    
    /**
     * Token 过期时间，默认 2 小时（毫秒）
     */
    private Long jwtExpiration = 7200000L;
}
```

**配置文件示例**：

```yaml title="application.yaml"
app:
  security:
    permit-all-urls:
      - /actuator/health
      - /actuator/info
      - /v3/api-docs/**
      - /swagger-ui/**
    jwt-secret: ${JWT_SECRET:default-secret-key-change-in-production}
    jwt-expiration: 7200000  # 2小时
```

### 3. JWT Token 提供者

```java title="blog-common/src/main/java/com/blog/common/security/JwtTokenProvider.java"
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final SecurityProperties securityProperties;
    
    /**
     * 生成 JWT Token（带用户ID）
     */
    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        
        // 提取角色信息
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        claims.put("roles", roles);
        claims.put("userId", userId);  // ✅ 存储用户ID便于后续获取
        
        return createToken(claims, userDetails.getUsername());
    }
    
    /**
     * 创建 Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 
                       securityProperties.getJwtExpiration()))
            .signWith(getSigningKey())  // ✅ 使用 SecretKey
            .compact();
    }
    
    /**
     * 验证 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())  // ✅ 现代API
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT Token 验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从 Token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    /**
     * 从 Token 中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }
    
    /**
     * 从 Token 中提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }
    
    /**
     * 从 Token 中解析所有 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwtSecret()
                                           .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

:::warning JJWT API 版本
项目使用 **JJWT 0.12.x** 现代API：
- ✅ `Keys.hmacShaKeyFor()` - 生成 `SecretKey`
- ✅ `.verifyWith()` - 验证签名
- ✅ `.parseSignedClaims()` - 解析Token

❌ 避免使用已废弃的API：
- ~~`.setSigningKey(String)`~~ - 已废弃
- ~~`SignatureAlgorithm.HS256`~~ - 已废弃
:::

### 4. JwtAuthenticationDetails 类

用于在认证详情中存储额外的用户信息（如userId）。

```java title="blog-common/src/main/java/com/blog/common/security/JwtAuthenticationDetails.java"
@Getter
public class JwtAuthenticationDetails extends WebAuthenticationDetails {
    
    private final Long userId;
    
    public JwtAuthenticationDetails(HttpServletRequest request, Long userId) {
        super(request);
        this.userId = userId;
    }
}
```

**使用场景**：在JWT过滤器中设置

```java
// 从JWT Token提取userId
Long userId = tokenProvider.getUserIdFromToken(jwt);

// 创建认证详情
JwtAuthenticationDetails details = new JwtAuthenticationDetails(request, userId);

// 设置到Authentication
UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(username, null, authorities);
authentication.setDetails(details);
```

### 5. JWT 认证过滤器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. 从请求头中获取 Token
            String jwt = getJwtFromRequest(request);
            
            // 2. 验证 Token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 3. 从 Token 中获取用户信息
                String username = tokenProvider.getUsernameFromToken(jwt);
                List<String> roles = tokenProvider.getRolesFromToken(jwt);
                
                // 4. 构建权限列表
                List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
                
                // 5. 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 6. 设置到 Security Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("已设置用户认证: username={}", username);
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }
        
        // 继续过滤链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中提取 JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
```

## 🔐 使用示例

### 1. 用户注册

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = authService.register(request);
        return Result.success(user);
    }
}

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    public UserDTO register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        // 2. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        
        // ✅ 使用 BCrypt 加密密码
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        userMapper.insert(user);
        
        return userConverter.toDto(user);
    }
}
```

### 2. 用户登录

```java
@PostMapping("/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return Result.success(response);
}

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    
    public LoginResponse login(LoginRequest request) {
        // 1. 认证用户名和密码
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
        
        // 2. 设置认证上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3. 获取用户角色
        List<String> roles = userService.getUserRoleKeys(userId);
        
        // 4. 生成 JWT Token
        String token = tokenProvider.generateToken(request.getUsername(), roles);
        
        // 5. 返回响应
        return new LoginResponse(token, userDTO);
    }
}
```

### 3. 保护接口

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 需要认证 - 任何已登录用户都可以访问
     */
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserDTO user = userService.getById(userId);
        return Result.success(user);
    }
    
    /**
     * 需要 ADMIN 角色
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.listAll();
        return Result.success(users);
    }
    
    /**
     * 需要 ADMIN 或 USER_MANAGER 角色
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO updated = userService.updateByDto(userDTO);
        return Result.success(updated);
    }
    
    /**
     * 自定义权限表达式
     */
    @PreAuthorize("hasRole('ADMIN') or #id == principal.userId")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
```

### 4. 获取当前用户信息

```java title="blog-common/src/main/java/com/blog/common/utils/SecurityUtils.java"
public class SecurityUtils {
    
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication != null && 
            authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        
        return null;
    }
    
    /**
     * 获取当前登录用户ID
     * 
     * 从 JwtAuthenticationDetails 直接获取，无需查询数据库
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication != null && 
            authentication.getDetails() instanceof JwtAuthenticationDetails details) {
            return details.getUserId();
        }
        
        return null;
    }
    
    /**
     * 获取当前用户角色列表
     */
    public static List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
    
    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }
}
```

:::tip 性能优化
`getCurrentUserId()` 从 `JwtAuthenticationDetails` 直接获取用户ID，**无需查询数据库**，性能优秀且线程安全。
:::

## ⚠️ 安全最佳实践

### 1. 密码安全

```java
// ✅ 正确：使用 BCrypt
String passwordHash = passwordEncoder.encode(plainPassword);

// ❌ 错误：明文存储
user.setPassword(plainPassword);

// ❌ 错误：使用 MD5（不安全）
String md5Hash = DigestUtils.md5Hex(plainPassword);
```

### 2. JWT Secret 配置

```yaml
# ❌ 错误：硬编码在配置文件
app:
  security:
    jwt-secret: mysecretkey123  # 太短且硬编码

# ✅ 正确：使用环境变量
app:
  security:
    permit-all-urls:
      - /actuator/health
      - /actuator/info
    jwt-secret: ${JWT_SECRET}  # 从环境变量读取
    jwt-expiration: 7200000
```

**生成安全的 Secret**：
```bash
# 生成256位随机字符串（至少32字节）
openssl rand -base64 32

# 设置环境变量（Linux/Mac）
export JWT_SECRET="生成的随机字符串"

# 设置环境变量（Windows）
set JWT_SECRET=生成的随机字符串
```

### 3. Token 传递

```bash
# ✅ 正确：使用 Authorization Header
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  http://localhost:8080/api/users/me

# ❌ 错误：在 URL 中传递 Token
curl http://localhost:8080/api/users/me?token=eyJhbGc...
```

### 4. 错误处理

```java
// ✅ 正确：不泄露敏感信息
if (!passwordEncoder.matches(password, user.getPasswordHash())) {
    throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
}

// ❌ 错误：泄露用户是否存在
if (user == null) {
    throw new BusinessException("用户不存在");
}
if (!passwordEncoder.matches(password, user.getPasswordHash())) {
    throw new BusinessException("密码错误");
}
```

## 📚 延伸阅读

<!-- 以下页面即将推出 -->
- **JWT 认证详解** - JWT 工作原理和实现
- **密码加密** - BCrypt 原理和使用

---

**安全提醒**：安全是一个持续的过程，定期审查和更新安全配置！
