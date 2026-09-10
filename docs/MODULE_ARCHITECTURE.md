# miyf 服务端模块架构

> 单体启动（`server-boot`）+ Maven 多模块边界。

## 总览

```
server/
├── lib-core/                 # 平台横向能力（pom）
│   ├── core-common           # ApiResult / 数据源 / CacheClient(Lettuce) / MyBatis
│   ├── oss-service           # MinIO / 本地存储 ✅
│   ├── notification-service  # 邮件 / 短信 / 站内信落库 ✅
│   ├── dictionary-service    # 数据字典 ✅
│   ├── config-service        # 系统配置 ✅
│   ├── monitor-service       # JVM / Redis / DB 概览 ✅
│   ├── job-service           # 定时任务启停 / 手动触发 ✅
│   └── search-service        # Elasticsearch 搜索封装（暂时关闭，走 SQL） ✅
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
kitchen → auth + oss + core-common（可按需依赖 search-service）
health → auth + job + core-common
notification/job/monitor → auth + core-common
search → core-common
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
| search-service              | ✅ Elasticsearch 门面（**暂时关闭**，Noop + SQL 查询；`APP_SEARCH_ENABLED=true` 可再启用） |
| core-common 缓存            | ✅ `CacheClient`（Lettuce）：对象 / 列表 / 树；关闭时 Noop；`RedisJsonCache` 兼容委托 |

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
      mock-enabled: false      # true=演示数据；false=调真实 Health Kit；prod/docker 强制 false
      client-id: ${HUAWEI_HEALTH_CLIENT_ID:}
      client-secret: ${HUAWEI_HEALTH_CLIENT_SECRET:}
      redirect-uri: http://localhost:5173/health/providers
  search:
    enabled: false            # 连接装配：true 注入真实 ES 客户端，否则 NoopSearchClient
    index-prefix: miyf
    uris:
      - http://localhost:9200
  redis:
    enabled: true             # false 时注入 Noop CacheClient
    cache:
      category-ttl-seconds: 600
      hot-dish-ttl-seconds: 300
      empty-ttl-seconds: 60
      tree-ttl-seconds: 600     # 菜单/组织树等
      object-ttl-seconds: 300
# spring.data.redis.lettuce.pool.* 控制 Lettuce 连接池（见 application.yml）
```

业务注入 `CacheClient`：`objects(T)` / `lists(E)` / `trees(N)` / `values(TypeReference)`；厨房分类与热门菜已走 `lists`。

搜索召回：`SearchQueries` 将 `AbstractCondition`（page/rows/keyword/sort）转为 `SearchQuery`；业务只拿 `SearchIdPage` 回表。
**当前暂时不启用 ES**：`DishSearchIndexService.isRecallEnabled()` 恒为 false，用户端列表/热门/推荐走 SQL；
`APP_SEARCH_ENABLED` 保持 `false`，compose 已移除 elasticsearch 服务。恢复时需重新接入 ES 容器并改回召回开关。

生产环境变量见 `deploy/.env.example`（`APP_HEALTH_PROVIDER_HUAWEI_ENABLED`、`HUAWEI_HEALTH_*`、`APP_SEARCH_ENABLED`、`REDIS_LETTUCE_*`）。华为 OAuth 的 Redis 仅存 access/refresh
token，不落 clientSecret。

## 数据范围（DataScope）

`DataScopeService`（`organization-service`）约束 IAM，并已接入健康主体。

| 域 | 是否按组织裁剪 | 说明 |
|----|----------------|------|
| IAM（sys_user / org / role users） | ✅ | 角色 `data_scope`：ALL / SELF / ORG / ORG_CHILD |
| 健康主体 / 采样 / 绑定 / 同步 | ✅ | `health_subject.org_unit_id` + `created_by`；SELF 按创建人，ORG 系按组织 |
| 厨房订单 / 菜品 / kitchen_user | ❌ | `kitchen_user` 无 `org_unit_id`，与 `sys_org_unit` 未绑定 |

健康管理端查询、更新、删除、同步与华为 OAuth 均调用 `requireAccessibleSubject`；定时任务无登录上下文时仅校验主体存在。
历史主体（`org_unit_id`/`created_by` 为空）由 `V23__health_subject_datascope_backfill.sql` 按组织编码 `HQ`、用户名 `admin` 动态解析后回填，并在 remark 标记 `[datascope-backfill]`。

## 包命名约定

| 模块 | 包前缀 |
|------|--------|
| auth / security 注解与 JWT | `cn.miyf.auth.*` |
| permission | `cn.miyf.permission.*` |
| organization | `cn.miyf.organization.*` |
| user | `cn.miyf.user.*` |
| gateway（过滤链，非独立网关） | `cn.miyf.gateway.*` |
| oss 上传 / 签名读文件 | `cn.miyf.oss.*`（上传权限码 `file:upload`；`GET /r/{id}` 签名 URL） |
| kitchen / health | `cn.miyf.kitchen.*` / `cn.miyf.health.*` |

历史提示词与过时 `clumsy_kitchen` 计划见 [`archive/`](archive/)。
