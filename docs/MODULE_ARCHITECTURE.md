# miyf 服务端模块架构

> 单体启动（`server-boot`）+ Maven 多模块边界。

## 总览

```
server/
├── lib-core/                 # 平台横向能力（pom）
│   ├── core-common           # ApiResult / 数据源 / Redis / MyBatis
│   ├── oss-service           # MinIO / 本地存储 ✅
│   ├── notification-service  # 邮件 / 短信 / 站内信落库 ✅
│   ├── dictionary-service    # 数据字典 ✅
│   ├── config-service        # 系统配置 ✅
│   ├── monitor-service       # JVM / Redis / DB 概览 ✅
│   └── job-service           # 定时任务启停 / 手动触发 ✅
├── lib-auth/                 # 认证鉴权（pom）
│   ├── gateway-service       # Security 过滤链 / 限流 / 方法鉴权 ✅
│   ├── auth-service          # 登录 / JWT / 注解 / 账号表 ✅
│   ├── organization-service  # 组织 ✅
│   ├── permission-service    # RBAC / Bootstrap / Permission 服务 ✅
│   └── user-service          # 用户 / Me ✅
├── lib-app/
│   ├── kitchen-service
│   └── health-service
└── server-boot/
```

## 依赖方向

```
server-boot → 各 *-service + core-common

config/dictionary → auth-service → core-common
permission → auth-service → core-common
gateway → permission + auth
organization/user → permission
kitchen → auth + oss + core-common
health → auth + job + core-common
notification/job/monitor → auth + core-common
```

## 迁移动作状态

| 模块                          | 状态                                                                  |
|-----------------------------|---------------------------------------------------------------------|
| oss / config / dictionary   | ✅                                                                   |
| auth / gateway / permission | ✅                                                                   |
| organization / user         | ✅ 已拆 `OrganizationApplicationService` / `SysUserApplicationService` |
| notification-service        | ✅ 站内信落库 + SMTP 适配器（开关）+ 日志短信                                        |
| monitor-service             | ✅ JVM/磁盘/线程/CPU + Redis/DB 概览 + 管理端页                                |
| job-service                 | ✅ 启停持久化 + 手动触发 + 列表 API                                             |

管理端：`/system/monitor`、`/system/jobs`；健康业务：`/health/overview|subjects|samples|providers|trends`（Flyway V7～V9 菜单）。

定时任务：`system.heartbeat`、`health.provider.sync`（`app.health.sync.cron`，默认每小时 :15）。

## 关键 API

- 通知：`/api/admin/system/notifications/**`（发送 / 站内信 / 已读）
- 任务：`/api/admin/system/jobs`、`/{code}/trigger|enable|disable`
- 监控：`/api/admin/system/monitor/overview`

## 配置摘录

```yaml
app:
  notification:
    mail:
      enabled: false          # true 时需配合 spring.mail.*
      from: noreply@miyf.local
    sms:
      enabled: false
      provider: logging
  health:
    providers:
      huawei:
        enabled: false        # 打开后侧栏可授权/同步
    sync:
      cron: "0 15 * * * *"    # HealthSyncJob；系统任务页可启停
    huawei:
      mock-enabled: true      # true=演示数据；false=调真实 Health Kit
      client-id: ${HUAWEI_HEALTH_CLIENT_ID:}
      client-secret: ${HUAWEI_HEALTH_CLIENT_SECRET:}
      redirect-uri: http://localhost:5173/health/providers
```

生产环境变量见 `deploy/.env.example`（`APP_HEALTH_PROVIDER_HUAWEI_ENABLED`、`HUAWEI_HEALTH_*`）。Redis 仅存 access/refresh
token，不落 clientSecret。
