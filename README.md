# miyf

多业务平台（当前已接入厨房业务 `kitchen-service`、健康业务 `health-service`）。

> **当前版本（1.0.0）完全不涉及金额、支付和商业交易。**  
> 时区统一为 **Asia/Shanghai（UTC+8）**。

**miyf** 提供统一 IAM、安全与基础设施；厨房业务作为 `kitchen-service` 模块：家人或小团队预约做饭、点评，没有购物车与结账。

---

## 产品定位

| 项目 | 说明 |
|------|------|
| 平台 | miyf（IAM、菜单、权限、文件、鉴权） |
| 厨房业务 | 家庭 / 宿舍 / 小团队内部预约做饭 |
| 用户端 | 微信小程序（浏览、预约、评价） |
| 管理端 | Web 后台 `miyf-admin`（菜品、菜谱、预约、评价、权限） |
| 明确不做 | 价格、支付、优惠券、积分、电商交易 |

## 核心功能

- 分类与菜品浏览（上架、热门、推荐）
- 结构化菜谱（食材 / 步骤）
- 预约下单：事务 + 有限库存原子扣减；取消回补
- 预约状态机（待确认 → … → 完成 / 取消）
- 评价资格校验、防重复评价、评分重算（隐藏/恢复后）
- 管理员 JWT + RBAC；操作日志脱敏
- Redis：分类/热门缓存、登录辅助限流、分布式锁
- 文件上传：默认 **MinIO**（校验 MIME / 魔数 / 大小）
- Docker Compose + Nginx 一键编排

## 视觉风格

暖色厨房风：奶油底 `#FFFDF8`、主强调 `#FFB36B`、辅色薄荷 `#9DD9C4`，圆角卡片、生活化文案。管理端与小程序共用同一套 Token 气质。

## 系统架构

```text
微信小程序 / 管理后台
        │
     Nginx (:80)
   ┌────┼────┐
   │    │    │
  /api /media  静态 Admin
   │    │
   ▼    ▼
 Spring Boot ──► MinIO
   │
   ├── PostgreSQL (PRIMARY · HikariCP · Flyway)
   └── Redis
```

- Domain / Application **不绑定**具体数据库厂商；SQL 方言与 JSONB 落在 Infrastructure。
- 多数据源骨架：PRIMARY 默认 PostgreSQL；SECONDARY 可配置、默认关闭。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | JDK 21 · Spring Boot **4.0.8** · Spring Security · MyBatis-Plus · HikariCP · Flyway · Redis · MinIO · springdoc OpenAPI |
| 小程序 | Taro 4 · React · TypeScript · Zustand · Sass |
| 管理端 | React 18 · Vite 6 · Ant Design 5 · ECharts · Zustand |
| 部署 | Docker Compose · Nginx · PostgreSQL 16 · Redis 7 · MinIO |

> 选用 Spring Boot 4.0.8：与 `mybatis-plus-spring-boot4-starter` 对齐，避免 SNAPSHOT/RC。连接池使用 Boot 自带的 HikariCP。

## 项目目录

```text
miyf/
├── server/                 # 后端（Maven 多模块）
│   ├── lib-core/           # 平台横向：core-common + oss/dict/config/…-service
│   ├── lib-auth/           # 认证鉴权：gateway/auth/org/permission/user-service
│   ├── lib-app/            # 业务：kitchen-service / health-service
│   ├── server-boot/        # 启动入口 + Flyway + yml
│   └── Dockerfile
├── miniapp/                # 微信小程序（厨房业务）
├── miyf-admin/             # 管理后台（Vite）
├── sql/                    # 参考 SQL（正式迁移在 Flyway）
├── deploy/
│   ├── docker-compose.yml
│   ├── nginx/
│   └── .env.example
├── docs/                   # MODULE_ARCHITECTURE、计划与设计摘录
├── .env.example
└── README.md
```

## 环境要求

| 组件 | 建议版本 |
|------|----------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| PostgreSQL | 16+ |
| Redis | 7+ |
| MinIO | 最新稳定版 |
| 微信开发者工具 | 最新稳定版 |
| Docker / Compose | 可选，用于一键部署 |

---

## 数据库与中间件

| 项 | 说明 |
|----|------|
| 默认数据库 | **PostgreSQL** |
| 连接池 | **HikariCP**（PRIMARY / SECONDARY 各自独立） |
| ORM / DAO | **MyBatis-Plus**（复杂 SQL 放 Mapper XML） |
| 缓存 | **Redis** |
| 迁移 | **Flyway**（`classpath:db/migration/postgresql`） |

> PostgreSQL 是当前默认实现，**不是**业务层永久绑定的数据库。切换厂商时主要改 Infrastructure、DataSource、SQL/Dialect，而不是 Domain / Service。

### 多数据源

```text
当前默认：
PRIMARY  → PostgreSQL → HikariCP

架构预留：
SECONDARY → 可配置（MySQL / MariaDB / Oracle / SQL Server 等）→ HikariCP
            默认 SECONDARY_DB_ENABLED=false
```

未来扩展时仍保持：**Domain / Application 不感知库类型**。

---

## 本地开发

### 1. 环境变量

```bash
cp .env.example .env
```

按需修改数据库、Redis、JWT、微信、MinIO 等。**真实密钥勿提交 Git。**

本地直连 MinIO 时：

```text
FILE_STORAGE_BASE_URL=http://localhost:9000/miyf
FILE_STORAGE_ENDPOINT=http://localhost:9000
```

经 Compose + Nginx 时改为：

```text
FILE_STORAGE_BASE_URL=http://localhost/media
FILE_STORAGE_ENDPOINT=http://minio:9000
```

### 2. 后端启动

要求 **JDK 21**（`JAVA_HOME` 指向 JDK 21；若 `mvn -v` 仍显示 Java 8，Maven 会因文本块/模式匹配编译失败）。

```bash
# 先启动 PostgreSQL、Redis、MinIO（本地或 Docker）
cd server
mvn -pl server-boot -am spring-boot:run
```

- API：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- 默认管理员（种子）：`admin` / `change-me`（以 `.env` 中 `ADMIN_SEED_*` 为准）

### 3. 管理后台

```bash
cd miyf-admin
npm install
npm run dev
```

开发地址通常为 `http://localhost:5173`（Vite 已代理 `/api` → `8080`）。

### 4. 微信小程序

```bash
cd miniapp
npm install
npm run dev:weapp
```

用微信开发者工具打开 `miniapp/dist`（或项目配置的输出目录）。开发阶段可开启 `WX_AUTH_MOCK_ENABLED=true`。

---

## Swagger / OpenAPI

| 入口 | 地址 |
|------|------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 分组 | **用户端** / **管理端** |

经 Nginx 时：

- `http://localhost/swagger-ui/`
- `http://localhost/v3/api-docs`

在 UI 右上角 **Authorize** 填入登录返回的 JWT（`Bearer` 前缀可不写，按 springdoc 提示即可）。微信登录、管理员登录接口已标记为无需鉴权。

---

## 文件上传

- 接口：`POST /api/upload`（`multipart` 字段名 `file`）
- 权限：`file:upload`
- 允许：JPG / PNG / WebP；默认最大约 5MB；服务端校验 Content-Type + 魔数，文件名由服务端生成
- 存储：默认 MinIO；可选 `FILE_STORAGE_TYPE=local`

---

## Docker / Compose / Nginx

完整步骤（安全组、环境变量、构建启动、验收、更新、备份、HTTPS、排障）见：

**[deploy/README.md](deploy/README.md)**

当前公网示例：`http://47.108.200.201/`（管理后台）· `http://47.108.200.201/api/`（API）。

快速命令：

```bash
# 本机构建（不在服务器编译）：
cd deploy
docker compose -f docker-compose.yml -f docker-compose.build.yml build
# 推仓库或 docker save 到服务器后：

# 服务器 /miyf/app/deploy：
cp .env.example .env
# 编辑 .env：密码、JWT、FILE_STORAGE_BASE_URL、MIYF_IMAGE_*
# 宿主机先建目录：/miyf/{app,data/...,log/server,log/nginx}（见 deploy/README.md 1.4）
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f server
```

编排服务：`postgres` · `redis` · `minio` · `server` · `nginx`。  
**默认不把数据库 / Redis / MinIO API 端口暴露到公网**，仅 Nginx `:80` 对外。

| 路径 | 说明 |
|------|------|
| `/` | 管理后台静态资源（miyf-admin） |
| `/api/` | 反代 Spring Boot |
| `/media/` | 反代 MinIO 公开对象 |
| `/actuator/health` | 健康检查 |
| `/swagger-ui/` | API 文档（可按生产需要关闭） |

### HTTPS

证书放在 `/miyf/data/nginx/certs/`，启用方式见 [deploy/README.md](deploy/README.md) 第 9 节。  
目录约定：代码 `/miyf/app`，数据 `/miyf/data/...`，日志 `/miyf/log/{server,nginx}`，见该文档第 1.4 / 1.5 节。

### 数据库初始化

容器内 Server 启动时 Flyway 自动迁移 + 种子数据。无需手工执行 `sql/`（`sql/` 仅作参考）。

---

## 生产部署要点

1. 修改所有 `change-me` 密钥（JWT、DB、MinIO、管理员密码）。
2. `WX_AUTH_MOCK_ENABLED=false`，配置真实 `WX_APP_ID` / `WX_APP_SECRET`。
3. 配置 HTTPS 与域名；生产关闭 Swagger（`APP_SECURITY_EXPOSE_DOCS=false` / `spring.profiles.active=prod`）。
4. 日志：应用默认输出到容器 stdout，配合 `docker compose logs` 或集中采集。
5. 监控：至少保留 `/actuator/health`；按需收紧 Actuator 暴露面。

### 微信小程序合法域名

在微信公众平台配置：

| 类型 | 示例 |
|------|------|
| request 合法域名 | `https://your-api-domain.com` |
| uploadFile 合法域名 | 同上（若小程序直传） |
| downloadFile 合法域名 | 图片 CDN / Nginx `/media` 所在域名 |

本地开发可在开发者工具中勾选「不校验合法域名」。

---

## 测试

```bash
cd server
mvn -pl server-boot -am test
```

覆盖：

- 单元：状态机、评分计算、菜谱结构、JWT、Redis/存储 Mock
- 集成（Testcontainers：PostgreSQL + Redis）：管理员/微信登录、HTTP 登录、RBAC、预约全链路、取消回补库存、评价资格与防重、评分 5→4.5→隐藏回 5、库存并发不超卖

需本机 Docker 可用；无 Docker 时 Testcontainers 用例会跳过。

前端：

```bash
cd miyf-admin && npm run typecheck && npm run build
cd miniapp && npm run build:weapp
```

---

## 常见问题

**Q: 上传成功但图片打不开？**  
检查 `FILE_STORAGE_BASE_URL` 是否与访问路径一致（本地直连 MinIO vs Nginx `/media`），以及桶是否公开读。

**Q: 管理后台 401？**  
确认已登录且 Token 未过期；权限不足会返回业务码 `40300`。

**Q: 微信登录失败？**  
开发环境确认 Mock 开关；生产确认 AppId/Secret 与合法域名。

**Q: 库存不足？**  
有限库存在并发下原子扣减，失败返回业务错误，不会超卖。

---

## 版本

| 组件 | 版本 |
|------|------|
| 产品 / API | **1.0.0** |
| miyf-admin / miniapp | 0.1.0（与产品同发布线） |

---

## 许可与声明

本仓库面向学习与内部非商业场景。再次强调：**不含支付、标价与交易能力**。
