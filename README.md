# shen-root 模块设计说明

> 框架示例文档，仅供参考

## 模块一览

```
shen-root (pom)                        聚合父工程
├── common                             通用基础
├── framework                          框架配置
├── security                           安全工具
├── module-auth                        认证模块
├── module-system1                     业务模块（共享认证）
├── module-system2                     业务模块（独立认证）
└── server                             单体启动器
```

## 各模块职责

| 模块 | 职责 | 核心内容 |
|---|---|---|
| common | 通用基础，不依赖任何框架 | ResultCode、BusinessException、hutool、fastjson2 |
| framework | 框架配置，通用基础设施 | RestTemplate、MyBatisPlusConfig、ShedLockConfig、GlobalExceptionHandler、BaseEntity、ApiLogAspect、ApiLogService、Actuator |
| security | 安全工具，纯工具不查表 | JWT过滤器、SecurityConfig、UserContext、RequirePermission注解、切面 |
| module-auth | 认证模块，只写一次 | AuthController、UserDetailsServiceImpl、SysUser、LoginUser |
| module-system1 | 业务模块（共享认证） | 依赖 module-auth，零认证代码 |
| module-system2 | 业务模块（独立认证） | 依赖 framework + security，自己实现 UserDetailsService |
| server | 启动入口 | 聚合 module-system1 + module-system2 |

## 依赖关系

```
shen-root
web + lombok（全局继承）
     │
     ├── common
     │   └── hutool + fastjson2
     │
     ├── security
     │   └── common + spring-security + jjwt
     │
     ├── framework
     │   └── common + mybatis-plus + RestTemplate + actuator
     │       ├── 配置：RestTemplateConfig、MybatisPlusConfig、ShedLockConfig
     │       ├── 异常：GlobalExceptionHandler
     │       ├── 基类：BaseEntity
     │       ├── 接口日志：ApiLogAspect + ApiLog实体 + ApiLogService（AOP切入，自动落库）
     │       └── 监控：Actuator 健康检查
     │
     ├── module-auth
     │   └── framework + security
     │
     ├── module-system1
     │   └── module-auth（framework、security 传递依赖）
     │
     ├── module-system2
     │   └── framework + security（自己实现 UserDetailsService）
     │
     └── server
         └── module-system1 + module-system2
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
└── aspect/
    └── ApiLogAspect.java        # @Around 切 Controller 层，采集数据后调用 ApiLogService.save 入库
```

- 切点：`execution(public * com.shen..controller..*.*(..))`
- 抓取：`joinPoint.getArgs()` 入参、`joinPoint.proceed()` 出参、`System.currentTimeMillis()` 耗时
- 落库：通过 `ApiLogService.save()` 落库
- 操作人信息：从 `JwtAuthenticationTokenFilter` 的 `request.setAttribute("userId")` 复用，避免重复解析 JWT
- 客户端信息：从 `request.getAttribute("clientVersion")` / `"clientPlatform"` 获取
- 字段截断：body/result 超长时截断为 3000 字符 + `...[TRUNCATED]`
- 当前方案：小项目直接落库，足够简单
- 可扩展：大项目可改为异步写入消息队列（Kafka/RocketMQ），或输出到 ELK 日志平台

## SQL 初始化方案

采用 Flyway，由 `framework` 模块引入 `flyway-mysql`（版本由 `spring-boot-starter-parent` 管理）。

### 每个有表的模块，各自放自己的 SQL

```
framework/src/main/resources/db/migration/
├── V20260708.100__api_log.sql          # 日期.100
└── V20260708.101__shedlock.sql         # 日期.101

module-auth/src/main/resources/db/migration/
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
| module-auth | 200 ~ 299 |
| module-system1 | 300 ~ 399 |
| module-system2 | 400 ~ 499 |

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

## 两种认证方式

| 方式 | 模块 | 直接依赖 | 认证代码 |
|---|---|---|---|
| 共享认证 | module-system1 | module-auth | 零行 |
| 独立认证 | module-system2 | framework + security | 自己写 UserDetailsService |

## 模块间调用

### 简单场景：单向依赖

接口定义在服务提供方，调用方直接依赖该模块：

```
// module-auth 定义接口
public interface IUserService {
    UserVO getUserById(Long id);
}

// module-auth 实现
@Service
public class UserServiceImpl implements IUserService {
    public UserVO getUserById(Long id) { ... }
}

// module-system1 直接注入调用
@Service
public class OrderService {
    private final IUserService userService;
}
```

依赖关系：`module-system1 → module-auth`

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