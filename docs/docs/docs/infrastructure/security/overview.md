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
    participant Auth as 认证服务
    participant DB as 数据库
    
    Client->>Filter: 1. 发送请求(带Token)
    Filter->>Filter: 2. 提取JWT Token
    Filter->>Filter: 3. 验证Token签名
    Filter->>Filter: 4. 解析用户信息
    Filter->>Auth: 5. 加载用户权限
    Auth->>DB: 6. 查询角色权限
    DB-->>Auth: 7. 返回权限列表
    Auth-->>Filter: 8. 构建认证对象
    Filter->>Filter: 9. 设置Security Context
    Filter-->>Client: 10. 继续请求处理
```

## 🔒 核心组件

### 1. Security 配置类

```java title="blog-system-service/src/main/java/com/blog/system/config/SecurityConfig.java"
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级安全
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（使用JWT无需CSRF保护）
            .csrf(csrf -> csrf.disable())
            
            // 禁用 Session（无状态认证）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers("/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // 其他端点需要认证
                .anyRequest().authenticated()
            )
            
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class)
            
            // 异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler())
                .accessDeniedHandler(accessDeniedHandler())
            );
        
        return http.build();
    }
}
```

### 2. JWT Token 提供者

```java title="blog-system-service/src/main/java/com/blog/system/security/JwtTokenProvider.java"
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:7200000}")  // 默认2小时
    private long jwtExpiration;
    
    private final UserDetailsService userDetailsService;
    
    /**
     * 生成 JWT Token
     */
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }
    
    /**
     * 从 Token 中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return (List<String>) claims.get("roles");
    }
    
    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("无效的JWT签名: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("无效的JWT Token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims字符串为空: {}", e.getMessage());
        }
        return false;
    }
}
```

### 3. JWT 认证过滤器

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

```java
@Component
public class SecurityUtils {
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return authentication.getName();
    }
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        String username = getCurrentUsername();
        
        // 从数据库查询用户ID（可以加缓存）
        // 或者从 JWT Token 的 claims 中获取
        return userService.getIdByUsername(username);
    }
    
    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
```

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
jwt:
  secret: mysecretkey123

# ✅ 正确：使用环境变量
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:7200000}
```

**生成安全的 Secret**：
```bash
# 生成256位随机字符串
openssl rand -base64 32
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
