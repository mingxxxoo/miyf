# 登录风控 · 权限体系 · 模块演进方案

> 版本 1.0 · 2026-09-06 · miyf

---

## 1. 管理端登录风控（已落地方向）

### 1.1 失败提示

- **位置**：登录卡片内、表单上方 `Alert`（不再用右上角 toast 作为主提示）
- **文案**：后端返回的业务 message；锁定时附剩余时间

### 1.2 验证码与封禁阶梯（按用户名，24h 累计窗口）

| 累计失败次数 | 策略           |
|--------|--------------|
| 1～2    | 仅表单内错误提示     |
| 3～5    | **必须**图形验证码  |
| 6      | 封禁 **5 分钟**  |
| 7      | 封禁 **10 分钟** |
| 8      | 封禁 **30 分钟** |
| 9      | 封禁 **2 小时**  |
| ≥10    | 封禁 **24 小时** |

规则补充：

- 窗口：失败计数 TTL = 24h；成功登录清零失败计数与锁定
- 锁定期间禁止登录（即使验证码正确）
- 验证码一次性；错误或过期需刷新
- Redis Key：`ck:login:fail:{principal}` / `ck:login:lock:{principal}` / `ck:captcha:{id}`

接口：

- `GET /api/admin/auth/login-status?username=` → 是否需验证码 / 是否锁定
- `GET /api/admin/auth/captcha` → captchaId + base64 图
- `POST /api/admin/auth/login` 增加 `captchaId` / `captchaCode`

错误码：

- `41006` 用户名或密码错误（data 可带 `captchaRequired`）
- `41011` 需验证码 / 验证码错误
- `41012` 账号临时锁定（data 带 `lockedUntil`）

---

## 2. 权限组与权限编码（@PopedomGroup）

### 2.1 注解（配置驱动，禁止用名称字符串判断域）

```java
@PopedomGroup(
    value = "11010000",
    scope = PopedomScope.PERSONAL,   // PERSONAL | ORG | SUPER
    service = "厨房服务",            // 组名 = scope.label + "-" + service → 个人-厨房服务
    product = "kitchen",
    roles = { KitchenRoleCodes.DEFAULT }
)
@RestController
public class XxxController {
    @MiyfPermission(code = "...") // 扫描生成 API 节点
}
```

- 鉴权：`popedom.scope() == PopedomScope.PERSONAL`，**禁止** `"个人".equals(name)`。
- 角色：模块内 `@PopedomRole` / `@PopedomRoles` 声明；超管 `PlatformRoles.SUPER_ADMIN` 在公共模块。
- 启动：`PermissionBootstrap` **清空 → 扫描 → 重建**（角色、权限组、权限树、角色↔组绑定；`admin` 重绑超管）。

### 2.2 编码规则（kitchen 示例）

| 权限域 | 8 位组编码 | 展示组名 | 绑定角色 |
|-----|------------|----------|----------|
| PERSONAL | `11010000` | 个人-厨房服务 | `KITCHEN_DEFAULT` |
| ORG | `11020000` | 单位-厨房服务 | `KITCHEN_ORG` |
| SUPER | `11030000` | 超管-厨房服务 | `SUPER_ADMIN` |

权限树层级（每组一棵，**无产品中间层**）：

```
个人                    ← ROOT（scope.label）
 └─ 预约管理            ← BIZ（@Tag / Controller 业务名）
     └─ 创建预约        ← API（@Operation.summary / code）
```

`tree_name`：`个人-预约管理-创建预约`（`scopeLabel-biz-apiName`）。

### 2.3 与现有模型关系

- 保留字符串 `code`（如 `kitchen:order:create`）供 JWT / `@RequirePermission` 使用
- `sys_perm_group.scope` 存 `PERSONAL|ORG|SUPER`
- `sys_perm_group.code` 对齐 8 位组编码；`@PopedomGroup.value` 为权威来源
- 超管角色绑定**全部**扫描到的权限组
---

## 3. 模块 `lib-auth`

已拆为子模块（见 `docs/MODULE_ARCHITECTURE.md`）：

- `gateway-service` — API 路由 / 鉴权入口 / 限流
- `auth-service` — 登录 / Token / SSO / OAuth2·OIDC
- `organization-service` — 部门 / 岗位 / 组织树
- `permission-service` — RBAC / 角色 / 权限
- `user-service` — 用户 / 账号 / 个人信息

实现仍大量位于 `core-common`，按包渐进迁入上述模块。

`kitchen-service` / `health-service` 依赖 `core-common`（及后续 lib-auth 子模块）  
`server-boot` 组装启动

---

## 4. 健康管理

模块 `lib-app/health-service`（原 `app-health`）：

- **通用健康管理**（体重、体征、趋势分析等），**不绑定华为或任一厂商**
- 数据模型：`health_subject` / `health_sample` / `health_provider_binding` / `health_sync_run`（Flyway V5）
- 规范指标：`HealthMetricCodes`（WEIGHT、HEART_RATE、STEPS…）
- 接入抽象：`HealthDataProvider` SPI + `HealthDataProviderRegistry`
    - 内置 `manual`（手动录入）、`example`（演示用，默认关闭）
    - 新厂商：实现 SPI → 映射为 `HealthSampleDraft` → 注册 Bean
- 配置前缀：`app.health.enabled`、`app.health.providers.<code>.enabled`

---

## 5. 系统设置页

- 仅 **系统管理员**（角色码 `SUPER_ADMIN` 或权限 `sys:settings:view`）在头像下拉出现「系统设置」
- 独立布局路由 `/system/*`：权限树、系统配置、数据字典
- 与厨房业务菜单隔离

---

## 6. 实施顺序

1. ✅ 登录风控 + 验证码 + 前端交互
2. ✅ `@PopedomGroup` + 16 位权限树扫描（`PermissionBootstrap` + Flyway V3）
3. ✅ 权限管理 UI 树形展示
4. ✅ `lib-auth` 子模块骨架（gateway/auth/org/permission/user）
5. ✅ 系统设置页骨架
6. ✅ `health-service` 通用健康骨架（无厂商绑定）
7. ✅ 系统配置 / 数据字典 CRUD（`sys_config` / `sys_dict_*` + `/api/admin/system/**`）
8. ✅ 健康数据模型 + 可插拔数据源抽象（`HealthDataProvider`）
9. ✅ 模块架构拆分（`docs/MODULE_ARCHITECTURE.md`）
10. ✅ 核心代码迁入各 `*-service`（OSS/配置/字典/鉴权/IAM）
11. ✅ `IamApplicationService` 拆为 org/user/permission；notification/monitor/job 落地
12. ✅ 站内信落库、SMTP 邮件适配器骨架、任务控制台启停与手动触发（Spring `@Scheduled`；Quartz/XXL 另议）
13. ✅ 系统监控控制台 UI + 指标增强；健康管理端概览/主体/采样页 + 菜单种子
14. ✅ 健康数据源绑定/同步控制台 + sync-runs API；菜单过滤支持权限码 `*`
15. ✅ 健康指标趋势 API + ECharts 趋势页（体重/心率等）
16. ✅ 华为 Health Kit Provider（OAuth + polymerize 同步；默认关闭，支持 mock）
17. ✅ 健康定时同步 Job（`health.provider.sync`，接入 job-service；按 ACTIVE 绑定增量拉取）
18. ✅ 华为 OAuth 生产硬化（deploy 环境变量/文档、拒绝授权提示、Redis 不落 clientSecret、删主体清 token）
