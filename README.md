# shen-root 模块设计说明

> 框架示例文档，仅供参考

## 模块一览

```
shen-root (pom)                        聚合父工程
├── common                             通用基础
├── framework                          框架配置
├── security                           安全工具
├── module-file                        文件管理模块（通用工具）
├── service-auth                       认证服务（业务服务）
├── service-xxx                        其他业务服务
└── server                             单体启动器
```

## 模块分类

| 类型 | 前缀 | 特点 | 模块 |
|---|---|---|---|
| **通用工具** | 无前缀 / module-* | 纯工具/配置，不含 Controller，不可独立运行 | common、framework、security、module-file |
| **业务服务** | service-* | 完整业务（Controller + Service + Entity），可独立部署 | service-auth、service-xxx |

## 各模块职责

| 模块 | 类型 | 职责 | 核心内容 |
|---|---|---|---|
| common | 通用基础 | 不依赖任何框架 | ResultCode、BusinessException、hutool、fastjson2 |
| framework | 框架配置 | 通用基础设施 | RestTemplate、MyBatisPlusConfig、ShedLockConfig、GlobalExceptionHandler、BaseEntity、ApiLogAspect、ApiLogService、Actuator |
| security | 安全工具 | 纯工具不查表 | JwtTokenUtil、JwtAuthenticationTokenFilter、UserContext、PasswordEncoder、AuthenticationManager |
| module-file | 通用工具 | 文件存储管理 | FileService、FileResourceService、FileStorageService、定时清理 |
| service-auth | 业务服务 | 认证服务（完整业务） | AuthController、用户/角色/菜单管理、PermissionLoadingFilter、SecurityConfig |
| service-xxx | 业务服务 | 其他业务服务 | 依赖通用工具模块，自己写 Controller + Service |
| server | 启动入口 | 聚合所有 service-* | 启动入口 |

## 依赖关系

```
shen-root
web + lombok（全局继承）
     │
     ├── common
     │   └── hutool + fastjson2
     │
     ├── security
     │   └── common + spring-security + hutool-jwt
     │       ├── 工具：JwtTokenUtil、ClientInfoUtil、UserContext
     │       ├── 过滤器：JwtAuthenticationTokenFilter（解析 token，设置 userId）
     │       └── 配置：SecurityConfig（提供 PasswordEncoder、AuthenticationManager）
     │
     ├── framework
     │   └── common + mybatis-plus + RestTemplate + actuator
     │       ├── 配置：RestTemplateConfig、MybatisPlusConfig、ShedLockConfig
     │       ├── 异常：GlobalExceptionHandler
     │       ├── 基类：BaseEntity
     │       ├── 接口日志：ApiLogAspect + ApiLog实体 + ApiLogService（AOP切入，自动落库）
     │       └── 监控：Actuator 健康检查
     │
     ├── module-file
     │   └── framework
     │       ├── 存储：FileStorageService（本地/OSS）
     │       ├── 业务：FileService、FileResourceService
     │       └── 定时：FileCleanupScheduledTask
     │
     ├── service-auth
     │   └── framework + security + module-file
     │       ├── AuthController（登录接口）
     │       ├── PermissionLoadingFilter（加载权限到 SecurityContext）
     │       ├── SecurityConfig（注册过滤器 + 权限规则）
     │       └── 用户/角色/菜单管理（Controller + Service）
     │
     ├── service-xxx
     │   └── framework + security + module-file（参考 service-auth 写法）
     │
     └── server
         └── service-auth + service-xxx
```

## 接口日志实现

```
framework 模块内：
├── entity/
│   └── ApiLog.java              # 日志实体：请求路径、入参、出参、耗时、操作人、客户端信息
├── mapper/
│   └── ApiLogMapper.java        # MyBatis-Plus Mapper
├── service/
│   ├── ApiLogService.java       # Service 接口
│   └── impl/
│       └── ApiLogServiceImpl.java  # Service 实现
├── aspect/
│   └── ApiLogAspect.java        # @Around 切 Controller 层，同步采集数据，异步落库
└── config/
    └── AsyncConfig.java         # @EnableAsync 异步配置
```

- 切点：`execution(public * com.shen..controller..*.*(..))`
- 抓取：`joinPoint.getArgs()` 入参、`joinPoint.proceed()` 出参、`System.currentTimeMillis()` 耗时
- 采集：同步提取日志数据（避免异步线程丢失 RequestContextHolder）
- 落库：异步执行 `@Async`，不阻塞接口响应
- 操作人信息：从 `JwtAuthenticationTokenFilter` 的 `request.setAttribute("userId")` 复用，避免重复解析 JWT
- 客户端信息：从 `request.getAttribute("clientVersion")` / `"clientPlatform"` 获取
- 字段截断：body/result 超长时截断为 3000 字符 + `...[TRUNCATED]`
- 异常安全：日志采集/落库失败不影响业务接口正常运行

## SQL 初始化方案

采用 Flyway，由 `framework` 模块引入 `flyway-mysql`（版本由 `spring-boot-starter-parent` 管理）。

### 每个有表的模块，各自放自己的 SQL

```
framework/src/main/resources/db/migration/
├── V20260708.100__api_log.sql          # 日期.100
└── V20260708.101__shedlock.sql         # 日期.101

service-auth/src/main/resources/db/migration/
└── V20260708.200__sys_user.sql         # 日期.200

module-system1/src/main/resources/db/migration/
└── V20260709.300__order.sql            # 日期.300

module-system2/src/main/resources/db/migration/
└── V20260710.400__xxx.sql              # 日期.400
```

### 版本号约定(可按实际情况自行决定变更)

`V{YYYYMMDD}.{号段+序号}__{描述}.sql`

| 模块 | 号段 |
|---|---|
| framework | 100 ~ 199 |
| service-auth | 200 ~ 299 |
| service-xxx | 300 ~ 399 |

```
V20260708.100__api_log.sql       # 0708创建，framework 第1个
V20260708.200__sys_user.sql      # 0708创建，auth 第1个
V20260715.201__sys_role.sql      # 0715创建，auth 第2个
```

- 日期知道创建时间，号段知道归属模块
- 同模块内递增：100 → 101 → 102

### server 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shen
    username: root
    password: xxx
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 注意

- `mysql-connector-j` 数据库驱动由 `framework` 模块引入
- Flyway 用 `flyway_schema_history` 表跟踪已执行的脚本，不会重复执行
- 清库重跑：`mvn flyway:clean flyway:migrate`（仅开发环境）

## 分布式定时任务

采用 ShedLock，基于数据库行锁。不用 Redis 做分布式锁的原因：Redis 主从切换可能丢锁，可靠性不如数据库。

#### 依赖与配置

放 `framework`，版本由父工程 `dependencyManagement` 统一管理：
```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
</dependency>
```

```java
// framework/config/ShedLockConfig.java
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
```

#### 锁表

framework 放 Flyway 脚本建表：
```sql
-- V20260708.101__shedlock.sql
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

#### 业务模块使用

```java
@Component
public class OrderTimeoutJob {

    @Scheduled(cron = "0 */5 * * * ?")
    @SchedulerLock(name = "orderTimeoutTask", lockAtMostFor = "5m")
    public void cancelTimeoutOrder() {
        // 多节点只有一个能抢到锁执行
    }
}
```

- 单体多节点、微服务多节点都适用
- 锁基于数据库，天然可靠

## 文件管理

### 设计思路

**核心问题**：文件上传后可能被多个业务引用（用户头像、订单附件、商品图片等），直接删除会导致其他业务失效。

**方案：引用计数 + 索引表**

```
file_resource 表（索引）
├── id: 雪花ID
├── path: 文件相对路径（如 /2026/07/09/123456.jpg）
├── ref_count: 引用计数
└── create_time: 上传时间
```

- **上传**：写入 `file_resource`，`ref_count = 1`
- **引用**：业务表存 `file_id`，调用 `FileService.add(fileId)` → `ref_count + 1`
- **取消引用**：业务删除记录时调用 `FileService.delete(fileId)` → `ref_count - 1`
- **物理清理**：定时任务扫描 `ref_count <= 0` 的记录，删除磁盘文件 + 数据库记录

### 图片压缩策略

按格式差异化处理，只压缩尺寸超限的图片（默认最大边长 2000px）：

| 格式 | 处理方式 | 说明 |
|---|---|---|
| PNG | pngquant 压缩 | 无损/有损压缩，保留透明度 |
| JPEG | Thumbnailator 动态质量 | 大文件 0.75，中文件 0.85，小文件 0.92 |
| WebP | Thumbnailator 缩放 | 质量 0.85 |
| BMP/GIF | 仅缩放 | 不压缩质量，避免 GIF 动图损坏 |

- 压缩后体积未减小则保留原文件
- 压缩失败自动降级到原文件
- 临时文件 finally 块清理

### 存储方案

**目录结构**

```
com.shen.file
├── service/                          # 业务服务层
│   ├── FileService.java             # 文件上传业务服务
│   └── FileResourceService.java     # 文件资源业务服务
├── storage/                          # 存储基础设施层
│   ├── FileStorageService.java      # 文件存储接口
│   └── impl/
│       ├── LocalFileStorageService.java      # 本地存储（已实现）
│       └── AliyunOssFileStorageService.java  # 阿里云 OSS（框架示例，未实现）
├── mapper/                           # 数据访问层
├── entity/                           # 实体类
└── dto/                              # 数据传输对象
```

**存储接口**

```java
public interface FileStorageService {
    void upload(InputStream inputStream, String path);
    void delete(String path);
    String getUrl(String path);
}
```

**实现状态**

| 存储类型 | 实现类 | 状态 | 说明 |
|---|---|---|---|
| 本地存储 | LocalFileStorageService | ✅ 已实现 | 默认使用，生产可用 |
| 阿里云 OSS | AliyunOssFileStorageService | ⚠️ 框架示例 | 仅展示扩展方式，未实现具体逻辑 |

**切换存储实现**

通过 `@ConditionalOnProperty` 注解实现配置化切换：

```yaml
files:
  storage:
    type: local  # 改为 aliyun-oss 即可切换（需先实现阿里云 OSS 逻辑）
```

| 配置值 | 实现类 | 说明 |
|---|---|---|
| `local` | LocalFileStorageService | 本地文件系统（默认，已实现） |
| `aliyun-oss` | AliyunOssFileStorageService | 阿里云 OSS（框架示例，需自行实现） |

**扩展新存储**

在 `storage.impl` 包下添加新实现类：

```java
@Component
@ConditionalOnProperty(name = "files.storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {
    // 实现接口方法
}
```

**配置说明**

| 配置项 | 说明 | 示例 |
|---|---|---|
| `files.upload.path` | 本地存储路径 | `/data/uploads` |
| `files.upload.url` | 文件访问前缀 | `https://cdn.example.com` |
| `files.upload.max-image-dimension` | 图片最大边长 | `2000` |
| `files.upload.thumb-size` | 缩略图尺寸 | `200` |
| `files.upload.cleanup-buffer-days` | 未引用文件清理缓冲天数 | `3` |
| `files.storage.type` | 存储类型 | `local` / `aliyun-oss` |

### 安全考虑

| 风险 | 防护 |
|---|---|
| 恶意文件伪装 | 扩展名 + `Content-Type` 双重校验，后续可加 Magic Number 校验 |
| 超大文件 | 配置 `max-file-size` 限制 |
| 敏感文件泄露 | 私有文件走网关鉴权，生成签名 URL |
| 文件名冲突 | 雪花ID 命名，不含原始文件名 |

### 缩略图

业务上经常需要列表页小图，上传时可同时生成缩略图：

```
原图：/2026/07/09/123456.jpg
缩略图：/2026/07/09/123456_thumb.jpg
```

缩略图尺寸可配置（如 200x200），与原图同目录存放。

### 定时清理

上传 3 天后仍未被引用（`ref_count <= 0`）的文件视为孤儿文件，定时清理：

```java
@Component
@EnableScheduling
@RequiredArgsConstructor
public class FileCleanupScheduledTask {

    private final FileResourceService fileResourceService;

    @Value("${files.upload.path}")
    private String uploadPath;

    @Value("${files.upload.cleanup-buffer-days:3}")
    private int cleanupBufferDays;

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "fileCleanupTask", lockAtMostFor = "1h")
    public void cleanupUnreferencedFiles() {
        Date bufferDate = DateUtil.offsetDay(new Date(), -cleanupBufferDays);
        // 分页查询 ref_count <= 0 且 create_time < bufferDate 的记录
        // 删除磁盘文件 + 删除数据库记录
    }
}
```

- 每天凌晨 1 点执行
- 3 天缓冲期：刚上传但还没关联业务的文件不会被误删
- 分页处理：每次 1000 条，避免一次性加载过多数据
- 文件不存在时只删数据库记录，不报错

## 认证服务（service-auth）

service-auth 是认证服务的完整实现，展示如何使用 security 模块实现完整的认证流程。其他业务服务（如 service-xxx）需要认证时，参考 service-auth 的写法即可。

### 核心组件

| 组件 | 说明 |
|---|---|
| AuthController | 登录接口，调用 AuthenticationManager 认证，生成 JWT token |
| JwtAuthenticationTokenFilter | security 模块提供，解析 token，设置 userId 到 request |
| PermissionLoadingFilter | 在 JwtAuthenticationTokenFilter 之后执行，通过 request.getAttribute("userId") 获取用户ID，查权限，设置到 SecurityContext |
| SecurityConfig | 注册过滤器 + 配置权限规则，启用 @EnableMethodSecurity 支持方法级权限注解 |

### 过滤器执行顺序

```
请求 → JwtAuthenticationTokenFilter（security 模块，解析 token，设置 userId）
     → PermissionLoadingFilter（service-auth，获取 userId，查权限，设置 SecurityContext）
     → Spring Security 权限校验（hasRole 等）
     → 业务 Controller
```

在 SecurityConfig 中注册：

```java
http
    .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
    .addFilterBefore(permissionLoadingFilter, JwtAuthenticationTokenFilter.class);
```

### 数据库表设计

| 表名 | 说明 | 关键字段 |
|---|---|---|
| sys_user | 用户表（核心实体） | id、status、create_time |
| sys_user_profile | 用户基础信息表（扩展） | id(同user.id)、nickname、avatar、real_name、gender、phone、email |
| sys_account | 账号表（登录凭证） | id、user_id、account_type、account_value、password、status |
| sys_role | 角色表 | id、role_code、role_name、role_type、description、status |
| sys_menu | 菜单/权限表 | id、parent_id、menu_name、permission_code、menu_type、path、icon、sort、status |
| sys_user_role | 用户角色关联表 | user_id、role_id |
| sys_role_menu | 角色菜单关联表 | role_id、menu_id |

### 账号类型

| 类型值 | 说明 |
|---|---|
| 1 | 用户名 |
| 2 | 手机号 |
| 3 | 邮箱 |
| 4 | 微信 |
| 5 | APP |

### 角色类型

| 类型值 | 说明 |
|---|---|
| 1 | 管理后台（有 ROLE_ADMIN 权限，可访问 /admin/**） |
| 2 | 小程序/APP |
| 3 | APP |

### 权限校验方式

**1. 路径配置（SecurityConfig）**

```java
.requestMatchers("/admin/**").hasRole("ADMIN")  // 需要 ROLE_ADMIN
.requestMatchers("/ws/**").permitAll()           // 无需认证
.anyRequest().authenticated()                    // 其他需登录
```

**2. 方法级注解（Controller）**

```java
@PreAuthorize("hasAuthority('user:delete')")
@DeleteMapping("/delete")
public ResponseEntity<Object> delete(Long id) { ... }
```

**3. 角色类型判断**

`PermissionLoadingFilter` 中调用 `SysRoleService.hasRoleType(userId, SysRole.ROLE_TYPE_ADMIN)`，如果用户有管理角色，赋予 `ROLE_ADMIN` 权限。

### Service 层设计

| Service | 核心方法 | 说明 |
|---|---|---|
| SysAccountService | findByAccount、login、register、bind、unbind、deleteByUserId、getAccountsByUserIds | 账号管理：登录、注册、绑定、解绑 |
| SysUserService | getPage、add、update、changeStatus、cancel | 用户管理：分页查询、增改、注销（软删除） |
| SysUserProfileService | getByUserId、updateInfo | 用户资料：查询、修改（含头像索引管理） |
| SysRoleService | getPage、add、update、delete | 角色管理：分页查询、增删改（删除时清理关联） |
| SysMenuService | getMenuTree、getPermissionsByUserId、add、update、delete | 菜单管理：树形结构、权限加载、递归删除 |
| SysRoleMenuService | assignMenus、getMenuIdsByRoleId、getMenuIdsByRoleIds、deleteByRoleId、deleteByMenuId | 角色菜单关联：分配权限、批量查询 |
| SysUserRoleService | getRoleIdsByUserId、assignRoles、deleteByUserId、deleteByRoleId | 用户角色关联：分配角色、批量删除 |

### 核心业务逻辑

**1. 登录即注册**
- 首次登录（数据库为空）自动注册为管理员角色
- 后续登录只认证，不注册
- 第三方登录无密码校验

**2. 注册流程**
- 创建 sys_user → 创建 sys_user_profile → 创建 sys_account → 关联 sys_user_role
- 检查账号值是否已存在，防止重复注册

**3. 注销逻辑**
- sys_user 标记为 STATUS_DELETED（软删除，保留数据）
- sys_account 物理删除（释放手机号/邮箱，允许新注册）

**4. 绑定/解绑**
- bind：检查账号值是否已被其他用户绑定
- unbind：至少保留一个账号，防止用户无账号

**5. 菜单树构建**
- 根据用户角色获取菜单ID（批量查询，去重）
- 按 parentId 递归构建树形结构

**6. 删除菜单**
- 递归删除所有子菜单及其角色菜单关联
- 事务保证原子性

**7. 删除角色**
- 删除角色记录
- 删除 sys_role_menu 关联
- 删除 sys_user_role 关联

**8. 头像索引管理**
- 修改头像时：旧头像减索引，新头像加索引
- 依赖 file 模块的 FileService

### 查询优化

| 场景 | 优化方式 |
|---|---|
| 用户分页查询 | JOIN sys_user_profile，批量查询 sys_account（避免 N+1） |
| 用户菜单查询 | 批量获取角色菜单ID（避免循环查询） |
| 权限查询 | 只查 permission_code 字段（减少数据传输） |
| 角色菜单查询 | select 只查 menuId 字段 |

### DTO 设计

| DTO | 用途 |
|---|---|
| SysUserDetailDTO | 用户详情聚合（User + Profile + Account 列表） |
| SysMenuTreeDTO | 菜单树形结构（含 children 递归） |

### 异常处理

| 异常类 | 处理内容 |
|---|---|
| AuthenticationExceptionHandler | 认证失败返回 401，权限不足返回 403 |

### 模块依赖

```
service-auth
├── framework（MyBatis-Plus、Flyway、全局异常）
├── security（JWT、PasswordEncoder、AuthenticationManager）
└── module-file（头像索引管理）
```

### service-xxx 业务服务写法

service-xxx 不依赖 service-auth，参考 service-auth 的写法，自己实现：

1. 创建 PermissionLoadingFilter（加载权限到 SecurityContext）
2. 创建 SecurityConfig（注册过滤器 + 配置权限规则）
3. 编写自己的 Controller + Service + Entity

## 业务服务写法

| 方式 | 模块 | 直接依赖 | 认证代码 |
|---|---|---|---|
| 共享认证 | service-xxx | service-auth | 零行 |
| 独立认证 | service-xxx | framework + security + module-file | 参考 service-auth 写法 |

## 模块间调用

### 简单场景：单向依赖

接口定义在服务提供方，调用方直接依赖该模块：

```
// service-auth 定义接口
public interface IUserService {
    UserVO getUserById(Long id);
}

// service-auth 实现
@Service
public class UserServiceImpl implements IUserService {
    public UserVO getUserById(Long id) { ... }
}

// service-xxx 直接注入调用
@Service
public class OrderService {
    private final IUserService userService;
}
```

依赖关系：`service-xxx → service-auth`

### 复杂场景：循环依赖（COLA 架构）

订单和用户互相调用时，Maven 报循环依赖。

**解法：每个业务模块拆成 api + impl**

```
module-order/
├── module-order-api/           # 对外接口（无依赖）
│   └── IOrderService.java
└── module-order-impl/          # 实现
    └── OrderServiceImpl.java

module-user/
├── module-user-api/
│   └── IUserService.java
└── module-user-impl/
    └── UserServiceImpl.java
```

**每个 impl 的依赖**：
```
order-impl 依赖：module-order-api(实现) + module-user-api(调用)
user-impl  依赖：module-user-api(实现)  + module-order-api(调用)
```

一句话：**impl 依赖两个 api——自己的用来实现，别人的用来调用**。

**依赖图**：
```
user-api  ←→  order-api     (api 之间零依赖，永不循环)
   ↑              ↑
user-impl      order-impl   (impl 依赖对方的 api)
```

**原理**：api 层只含接口 + DTO，不依赖任何人。impl 依赖别人的 api。所有箭头都是单向的。

### 拆微服务时

**单体阶段**：api 纯接口，不加任何路径注解，本地直接注入调用：

```java
// api：保持纯净，无注解
public interface IUserService {
    UserVO getUserById(Long id);
}

// 单体：本地注入，直接方法调用
@Autowired
private IUserService userService;
userService.getUserById(123L);
```

**改造微服务**：在 api 加上 `@RequestMapping` / `@GetMapping`，路径只写一次，然后：

```java
// api 加路径注解，只写一次
@RequestMapping("/api/user")
public interface IUserService {
    @GetMapping("/{id}")
    UserVO getUserById(@PathVariable Long id);
}

// 生产侧 impl：新增 Controller，实现接口，自动继承路径
@RestController
public class UserController implements IUserService {
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Override
    public UserVO getUserById(Long id) {
        return userServiceImpl.getUserById(id);
    }
}

// 消费侧 impl：新增 Feign，继承接口，自动继承路径
@FeignClient(name = "user-service")
public interface UserFeignClient extends IUserService {}
```

- 路径只写一次在 api，生产和消费都自动继承
- 模块间依赖从 Maven 依赖改为服务发现

## 命名规范

### DTO/VO 位置

- **模块专属 DTO/VO**：放在该模块的 `api` 包里，和接口定义在一起
- **全局共享 DTO**：放 `common/dto/`（如分页请求 `PageReq`、通用请求基类等）

```
module-user-api/
├── service/
│   └── IUserService.java       # 纯接口，不加任何路径注解
└── dto/
    ├── UserVO.java
    └── UserPageReq.java
```

业务模块内不要在 service/mapper 层拿数据库实体当 VO 返回，避免暴露表结构。

### 业务模块包结构

**先按领域划分，每个领域内再按层次划分**：

```
module-auth/
├── user/                          # 领域：用户
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   ├── IUserService.java
│   │   └── UserServiceImpl.java
│   ├── mapper/
│   │   └── UserMapper.java
│   └── entity/
│       └── User.java
└── role/                          # 领域：角色
    ├── controller/
    ├── service/
    ├── mapper/
    └── entity/
```

优点：内聚性好，删一个领域只删一个目录，结构清晰。

## 多环境配置

不在 jar 内打包配置文件，启动时通过外部 `config/` 目录覆盖：

```
部署目录/
├── app.jar
└── config/
    └── application.yml       # 外部配置，优先级高于 jar 内
```

拆微服务后通过注册中心（Nacos）管理配置，对代码无影响。

## 设计要点

- **common 不依赖框架**：纯工具，未来换技术栈也能复用
- **security 不查表**：JWT 校验、权限注解、UserContext 全是工具，不碰数据库
- **framework 管基础设施**：接口日志、全局异常、MyBatisPlus 配置、健康监控——所有业务无关的通用能力都在这
- **module-auth 只写一次**：共享模式直接依赖，独立模式参考实现
- **server 只聚合**：不写业务代码，拆微服务时给各模块加 Application.java 即可