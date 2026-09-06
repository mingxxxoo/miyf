# Docker Compose 部署指南

面向 **miyf** 的一键编排部署文档。  
当前公网示例服务器：`47.108.200.201`（无域名时用 IP；有域名后替换文中所有 IP 即可）。

编排目录：`deploy/`  
镜像构建：

| 服务 | 构建上下文 | Dockerfile |
|------|------------|------------|
| `server` | `server/` | `server/Dockerfile`（打包 `server-boot`） |
| `nginx` | 仓库根目录 | `deploy/nginx/Dockerfile`（构建 `miyf-admin` 静态资源） |

**生产服务器只 pull 镜像、不在服务器编译。** 本机构建使用 `docker-compose.build.yml`，再推仓库或 `docker save` 到服务器。

运行时服务：`postgres` · `redis` · `minio` · `server` · `nginx`。

```text
公网客户端
    │
    ▼ :80
  nginx
  ├─ /           → miyf-admin 静态页
  ├─ /api/       → server:8080
  ├─ /media/     → minio:9000/{bucket}/
  └─ /actuator/health → server
```

**默认不对公网暴露** PostgreSQL / Redis / MinIO API / Spring Boot 直连端口。

**宿主机目录一律落在 `/miyf` 下**（不再使用 `/opt/...`）：

```text
/miyf/
  app/                 # 部署代码（compose 工作目录：/miyf/app/deploy）
  data/                # 持久化数据（见 1.4）
  log/
    server/            # 后端：miyf.log + 按日 gzip（Logback）
    nginx/             # Nginx access/error；宿主机 logrotate 按日 gzip
```

---

## 1. 服务器准备

### 1.1 建议规格

- 系统：Ubuntu 22.04 / 24.04（或同等 Linux）
- 内存：≥ 4GB（推荐 8GB）
- 磁盘：≥ 40GB
- 已安装：Docker Engine 24+、Docker Compose 插件（`docker compose`）

### 1.2 云安全组 / 防火墙

| 端口 | 用途 | 是否对公网开放 |
|------|------|----------------|
| 22 | SSH | 建议限源 IP |
| 80 | HTTP（Nginx） | 是 |
| 443 | HTTPS（可选） | 有证书时再开 |
| 5432 / 6379 / 9000 / 8080 | 数据库/缓存/MinIO/直连 API | **否** |

示例（Ubuntu ufw）：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw enable
sudo ufw status
```

### 1.3 安装 Docker（Ubuntu 示例）

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker "$USER"
# 重新登录后再执行 docker 命令
docker version
docker compose version
```

### 1.4 宿主机目录（`/miyf/{app,data,log}`）

代码、数据、日志均挂在 **`/miyf`** 下；数据与日志用 bind mount 挂入容器（不再使用 Docker named volume）。

| 路径 | 用途 | Compose 变量 | 容器内挂载点 |
|------|------|--------------|--------------|
| `/miyf/app` | 部署代码（本仓库） | —（compose 在 `deploy/` 下执行） | — |
| `/miyf/data/db/data` | PostgreSQL 数据文件 | `MIYF_DATA_DB` | `/var/lib/postgresql/data` |
| `/miyf/data/redis/data` | Redis AOF | `MIYF_DATA_REDIS` | `/data` |
| `/miyf/data/oss/resource` | MinIO 对象存储 | `MIYF_DATA_OSS` | `/data` |
| `/miyf/data/nginx/certs` | HTTPS 证书（可选） | `MIYF_DATA_NGINX_CERTS` | `/etc/nginx/certs` |
| `/miyf/data/backup` | 手工备份落盘（不挂载） | — | — |
| `/miyf/log/server` | 后端应用日志（按日 + 按大小滚动并 `.gz`） | `MIYF_LOG_SERVER` / `LOG_PATH` | `/miyf/log/server` |
| `/miyf/log/nginx` | Nginx access/error | `MIYF_LOG_NGINX` | `/var/log/nginx` |

初始化（首次部署必做）：

```bash
sudo mkdir -p \
  /miyf/app \
  /miyf/data/db/data \
  /miyf/data/redis/data \
  /miyf/data/oss/resource \
  /miyf/data/nginx/certs \
  /miyf/data/backup \
  /miyf/log/server \
  /miyf/log/nginx

# PostgreSQL 官方镜像以 uid=70 写数据目录
sudo chown -R 70:70 /miyf/data/db/data
# Redis 镜像多为 uid=999
sudo chown -R 999:999 /miyf/data/redis/data
# MinIO 一般可用当前用户或 root 启动后自动处理；若权限报错再放宽：
sudo chmod -R 755 /miyf/data/oss/resource
# 后端容器用户 miyf（uid 1000）写日志
sudo chown -R 1000:1000 /miyf/log/server
sudo chmod -R 755 /miyf/log/nginx

sudo ls -la /miyf /miyf/data /miyf/log
```

### 1.5 安装 Nginx 日志按日压缩（logrotate）

后端日志由容器内 **Logback** 按日/按大小滚动并 gzip（文件形如 `miyf.2026-09-05.0.log.gz`）。  
Nginx 日志需在**宿主机**安装 logrotate：

```bash
sudo apt-get install -y logrotate   # 多数系统已自带
sudo cp /miyf/app/deploy/logrotate/miyf /etc/logrotate.d/miyf
sudo logrotate -d /etc/logrotate.d/miyf   # 干跑检查配置
```

切割后会通过 `docker compose exec nginx nginx -s reopen` 重新打开日志句柄（配置见 `deploy/logrotate/miyf`）。

---

## 2. 获取代码

任选其一。

### 方式 A：Git

```bash
sudo mkdir -p /miyf/app
sudo chown "$USER":"$USER" /miyf/app
cd /miyf/app
git clone <your-repo-url> .
```

### 方式 B：本机打包上传

在开发机仓库根目录：

```bash
# 排除 node_modules / target 等大目录后打包
tar --exclude='**/node_modules' --exclude='**/target' --exclude='**/dist' \
  --exclude='.git' --exclude='deploy/data' \
  -czf miyf.tgz .
scp miyf.tgz root@47.108.200.201:/miyf/
```

在服务器：

```bash
sudo mkdir -p /miyf/app
sudo tar -xzf /miyf/miyf.tgz -C /miyf/app
cd /miyf/app
```

---

## 3. 配置环境变量

```bash
cd /miyf/app/deploy
cp .env.example .env
nano .env   # 或 vim
```

### 3.1 必改项（生产）

| 变量 | 说明 |
|------|------|
| `POSTGRES_PASSWORD` | 数据库密码，勿用默认 |
| `JWT_SECRET` | ≥ 32 位随机串 |
| `FILE_STORAGE_ACCESS_KEY` / `FILE_STORAGE_SECRET_KEY` | MinIO 账号，勿用 `minioadmin` |
| `FILE_STORAGE_BASE_URL` | 对外可访问的媒体前缀 |

当前 IP 部署示例：

```env
FILE_STORAGE_BASE_URL=http://47.108.200.201/media
```

有域名后改为：

```env
FILE_STORAGE_BASE_URL=https://your-domain.com/media
```

### 3.2 微信相关

开发 / 冒烟可先保持 Mock：

```env
WX_AUTH_MOCK_ENABLED=true
```

正式小程序登录：

```env
WX_AUTH_MOCK_ENABLED=false
WX_APP_ID=你的AppId
WX_APP_SECRET=你的AppSecret
```

### 3.2.1 华为运动健康（OAuth）

默认关闭。需要在管理端拉取华为数据时：

1. 华为开发者联盟创建应用，开通 Health Kit，配置 **OAuth 客户端**（Client ID / Secret）
2. 回调地址填管理端「健康数据源」页完整 URL（须与 `HUAWEI_HEALTH_REDIRECT_URI` **完全一致**）
3. `.env` 示例：

```env
APP_HEALTH_PROVIDER_HUAWEI_ENABLED=true
APP_HEALTH_HUAWEI_MOCK=false
HUAWEI_HEALTH_CLIENT_ID=你的ClientId
HUAWEI_HEALTH_CLIENT_SECRET=你的ClientSecret
# 生产管理端示例（按实际域名改）
HUAWEI_HEALTH_REDIRECT_URI=https://www.miyf.cn/health/providers
# 本地 Vite 开发
# HUAWEI_HEALTH_REDIRECT_URI=http://localhost:5173/health/providers
```

4. `docker compose up -d server` 使环境变量生效
5. SUPER_ADMIN 重新登录以加载 `health:huawei:oauth` 等权限
6. 管理端：选主体 → **OAuth 授权** → 华为账号登录同意 → 回到本页完成绑定 → 同步或等待任务 `health.provider.sync`

说明：

- `APP_HEALTH_HUAWEI_MOCK=true` 时，未授权主体返回演示数据；已 OAuth 的主体仍走真实接口
- Token 按主体存 Redis，**不落 clientSecret**；撤销或删除主体会清理该主体 token
- 用户拒绝授权时 URL 带 `error` / `error_description`，管理端会提示并清掉查询参数

### 3.3 完整示例（请自行替换密钥）

见同目录 [`.env.example`](./.env.example)。核心片段：

```env
POSTGRES_DB=miyf
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<强密码>

JWT_SECRET=<至少32位随机串>
JWT_EXPIRE_SECONDS=604800

WX_AUTH_MOCK_ENABLED=true
WX_APP_ID=
WX_APP_SECRET=
SPRING_PROFILES_ACTIVE=docker

FILE_STORAGE_TYPE=minio
FILE_STORAGE_BUCKET=miyf
FILE_STORAGE_ACCESS_KEY=<minio用户>
FILE_STORAGE_SECRET_KEY=<minio密码>
FILE_STORAGE_BASE_URL=http://47.108.200.201/media
FILE_STORAGE_AUTO_CREATE_BUCKET=true
FILE_STORAGE_PUBLIC_READ=true

NGINX_HTTP_PORT=80

# 宿主机路径（与 1.4 节一致）
MIYF_APP_ROOT=/miyf/app
MIYF_DATA_DB=/miyf/data/db/data
MIYF_DATA_REDIS=/miyf/data/redis/data
MIYF_DATA_OSS=/miyf/data/oss/resource
MIYF_DATA_NGINX_CERTS=/miyf/data/nginx/certs
MIYF_LOG_SERVER=/miyf/log/server
MIYF_LOG_NGINX=/miyf/log/nginx
LOG_PATH=/miyf/log/server

# 阿里云 ACR（与 .env 中 MIYF_IMAGE_* 一致；版本号自行改）
MIYF_IMAGE_SERVER=registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
MIYF_IMAGE_NGINX=registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
MIYF_IMAGE_POSTGRES=registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine
MIYF_IMAGE_REDIS=registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine
MIYF_IMAGE_MINIO=registry.cn-shanghai.aliyuncs.com/miyf/minio:latest
```

> 种子管理员（Flyway）：用户名 `admin`，初始密码 `change-me`。**首次登录后请立即修改密码或改库中哈希。**

---

## 4. 镜像与启动（服务器只 pull）

启动前确认已执行 **1.4** 创建目录；建议执行 **1.5** 安装 logrotate。  
服务器上的 `docker-compose.yml` **不含 build**，只按 `.env` 里的 `MIYF_IMAGE_*` 拉镜像运行。

### 4.0 镜像一律进阿里云 ACR（禁止依赖 Docker Hub / Quay）

本机构建、服务器运行 **只使用**：

`registry.cn-shanghai.aliyuncs.com/miyf/<仓库>:<标签>`

不要在 compose / `.env` 里写 `docker.io/...`、`quay.io/...`。

**① 控制台创建仓库**（命名空间 `miyf`）：

| 仓库名 | 建议标签 | 用途 |
|--------|----------|------|
| `postgres` | `16-alpine` | 运行时 |
| `redis` | `7-alpine` | 运行时 |
| `minio` | `latest` | 运行时 |
| `maven` | `3.9.9-eclipse-temurin-21` | 本机构建 |
| `eclipse-temurin` | `21-jre-alpine` | 本机构建 |
| `node` | `20-alpine` | 本机构建 |
| `nginx` | `1.27-alpine` | 本机构建 |
| `miyf_app` / `miyf_nginx` | 如 `0.1.0` | 业务镜像 |

**② 首次入库：优先用 ACR「镜像同步 / 导入」**（阿里云代拉 Docker Hub，本机不必访问 docker.io）

容器镜像服务控制台 → **镜像工具** → **镜像同步**（或「导入镜像」）：

| 源（Docker Hub） | 目标（你的 ACR） |
|------------------|------------------|
| `library/postgres:16-alpine` | `miyf/postgres:16-alpine` |
| `library/redis:7-alpine` | `miyf/redis:7-alpine` |
| `minio/minio:latest` | `miyf/minio:latest` |
| `library/maven:3.9.9-eclipse-temurin-21` | `miyf/maven:3.9.9-eclipse-temurin-21` |
| `library/eclipse-temurin:21-jre-alpine` | `miyf/eclipse-temurin:21-jre-alpine` |
| `library/node:20-alpine` | `miyf/node:20-alpine` |
| `library/nginx:1.27-alpine` | `miyf/nginx:1.27-alpine` |

同步完成后，本机 / 服务器统一用 ACR 地址，例如 MinIO：

```text
registry.cn-shanghai.aliyuncs.com/miyf/minio:latest
```

**③ 若本机已有镜像，也可直接打标签推 ACR**（不再经过第三方仓库）

```powershell
docker login --username=peteroyu registry.cn-shanghai.aliyuncs.com

docker tag postgres:16-alpine registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine
docker tag redis:7-alpine registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine
# MinIO：请用控制台同步进 ACR；入库后本机只需：
docker pull registry.cn-shanghai.aliyuncs.com/miyf/minio:latest

docker push registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine
docker push registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine
```

构建用基座同理，已在 ACR 则：

```powershell
docker pull registry.cn-shanghai.aliyuncs.com/miyf/maven:3.9.9-eclipse-temurin-21
docker pull registry.cn-shanghai.aliyuncs.com/miyf/eclipse-temurin:21-jre-alpine
docker pull registry.cn-shanghai.aliyuncs.com/miyf/node:20-alpine
docker pull registry.cn-shanghai.aliyuncs.com/miyf/nginx:1.27-alpine
```

**④ `.env` 运行时 / 构建均指向 ACR**

```bash
MIYF_IMAGE_POSTGRES=registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine
MIYF_IMAGE_REDIS=registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine
MIYF_IMAGE_MINIO=registry.cn-shanghai.aliyuncs.com/miyf/minio:latest
MIYF_IMAGE_SERVER=registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
MIYF_IMAGE_NGINX=registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0

BASE_MAVEN_IMAGE=registry.cn-shanghai.aliyuncs.com/miyf/maven:3.9.9-eclipse-temurin-21
BASE_JRE_IMAGE=registry.cn-shanghai.aliyuncs.com/miyf/eclipse-temurin:21-jre-alpine
BASE_NODE_IMAGE=registry.cn-shanghai.aliyuncs.com/miyf/node:20-alpine
BASE_NGINX_IMAGE=registry.cn-shanghai.aliyuncs.com/miyf/nginx:1.27-alpine
```

> 个人「镜像加速器」不能代替 ACR 仓库；`docker compose` 与业务拉取 **只认上面 ACR 地址**。

### 4.1 本机构建镜像

在开发机 `deploy/` 目录（先完成 **4.0** 基础镜像入库）：

```bash
cd deploy
# 推荐：显式关掉 attestation，避免 ACR 拒收（compose 的 provenance/sbom 在部分 Desktop 版本上仍可能带上）
# PowerShell:
#   $env:BUILDX_NO_DEFAULT_ATTESTS="1"
#   docker buildx build --provenance=false --sbom=false --load ...
docker compose -f docker-compose.yml -f docker-compose.build.yml build
```

> 你当前环境是 `linux/x86_64`，与云服务器一致，**不要加** `--platform`。Apple Silicon 跨架构时可设 `$env:DOCKER_DEFAULT_PLATFORM="linux/amd64"`。
>
> `docker-compose.build.yml` 已关闭 `provenance`/`sbom`，避免阿里云 ACR 推送时报
`unknown manifest class for application/vnd.oci.empty.v1+json`。若 Compose 过旧不识别这两项，可改用：
> `docker buildx build --provenance=false --sbom=false ...` 或设 `$env:BUILDX_NO_DEFAULT_ATTESTS=1` 后再 build。

产物标签即：

- `registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:<版本>`（后端）
- `registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:<版本>`（管理端 + Nginx）

> ACR 命名空间 `miyf` 下请先创建镜像仓库 **`miyf_app`**、**`miyf_nginx`**（若你只用了 `miyf_app`，请再新建 `miyf_nginx`）。

### 4.2 推送到阿里云 ACR 后服务器 pull

**本机登录并推送：**

```bash
docker login --username=peteroyu registry.cn-shanghai.aliyuncs.com
# 按提示输入 ACR 固定密码（控制台「访问凭证」，不是阿里云登录密码）

cd deploy
# 若 .env 中版本已是目标版本，build 后直接 push：
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
```

若本地已有未带仓库前缀的镜像，可手动打标签再推：

```bash
docker tag <ImageId或本地名> registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
docker tag <ImageId或本地名> registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
```

**服务器侧完整步骤见 4.3**（登录 ACR → 配置 `.env` → pull → up）。

**备用：无仓库时** 本机 `docker save` → 服务器 `docker load`（镜像 tag 须与服务器 `.env` 中 `MIYF_IMAGE_*` 一致），再按 4.3 启动（可跳过对业务镜像的 `pull`）。

### 4.3 服务器启动（不编译）— 完整清单

服务器**不编译、不执行** `docker-compose.build.yml` / `build`。只需 Docker + Compose、部署目录与数据/日志目录。本机已完成 **4.1 / 4.2**（`miyf_app`、`miyf_nginx` 已 push 到 ACR）。

#### 前置条件

| 项 | 说明 |
|----|------|
| Docker | 已装 Engine + `docker compose`（见 **1.3**） |
| 安全组 / 防火墙 | 放行 22、80（见 **1.2**） |
| 宿主机目录 | 已执行 **1.4**（含 `/miyf/log/...` 权限） |
| 代码 / 配置 | 至少有下方「最小文件集」 |
| ACR | 本机已 push 对应版本的 `miyf_app`、`miyf_nginx` |

#### 服务器上需要的最小文件

不必上传整个源码仓库；`/miyf/app/deploy` 至少包含：

```text
/miyf/app/deploy/
├── docker-compose.yml      # 必须（仅 image，无 build）
├── .env                    # 必须（生产配置，勿提交公钥仓库）
├── logrotate/miyf          # 建议（安装见 1.5）
└── （无需 docker-compose.build.yml、无需 server/、miyf-admin/ 源码）
```

可用 git 只检出 deploy，或从本机拷贝：

```bash
# 本机示例
scp docker-compose.yml .env.example root@47.108.200.201:/miyf/app/deploy/
scp -r logrotate root@47.108.200.201:/miyf/app/deploy/
```

#### 逐步操作

**1）目录与权限（若尚未做）**

```bash
sudo mkdir -p /miyf/app/deploy \
  /miyf/data/db/data /miyf/data/redis/data /miyf/data/oss/resource \
  /miyf/data/nginx/certs /miyf/data/backup \
  /miyf/log/server /miyf/log/nginx
sudo chown -R 70:70 /miyf/data/db/data
sudo chown -R 999:999 /miyf/data/redis/data
sudo chown -R 1000:1000 /miyf/log/server
sudo chmod -R 755 /miyf/data/oss/resource /miyf/log/nginx
```

**2）准备 `.env`**

```bash
cd /miyf/app/deploy
cp .env.example .env   # 若已有 .env 则跳过
nano .env              # 或 vim
```

服务器 `.env` **必核**：

| 变量 | 要求 |
|------|------|
| `MIYF_IMAGE_SERVER` / `MIYF_IMAGE_NGINX` | 与本机 push 的业务镜像 tag **完全一致** |
| `MIYF_IMAGE_POSTGRES` / `REDIS` / `MINIO` | 均指向 ACR，**不要**再用 `docker.io/...` |
| `POSTGRES_PASSWORD` | 强密码；与首次初始化后勿随意改（改则库连不上） |
| `JWT_SECRET` | ≥32 位随机串 |
| `FILE_STORAGE_ACCESS_KEY` / `SECRET_KEY` | MinIO 账号；与 compose 注入一致 |
| `FILE_STORAGE_BASE_URL` | 公网可访问，如 `http://47.108.200.201/media`（勿写 `:9000`） |
| `MIYF_DATA_*` / `MIYF_LOG_*` / `LOG_PATH` | 与 **1.4** 目录一致（默认即可） |

> `BASE_*_IMAGE` 仅本机构建用，服务器可不改，不影响 `up`。

**3）登录 ACR（私有仓库必须）**

```bash
docker login --username=peteroyu registry.cn-shanghai.aliyuncs.com
# 密码：ACR 控制台「访问凭证」里的固定密码
```

**4）确认已登录 ACR**（`docker login` 见上一步；重启 Docker 后需重新 login）

**5）拉取镜像并启动**

`docker compose pull` 会按 `docker-compose.yml` + `.env` 拉取下列 **5** 个镜像（**全部为 ACR，不访问 docker.io**）：

| 服务 | 完整镜像地址（默认 `.env`） | 变量 |
|------|------------------------------|------|
| `postgres` | `registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine` | `MIYF_IMAGE_POSTGRES` |
| `redis` | `registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine` | `MIYF_IMAGE_REDIS` |
| `minio` | `registry.cn-shanghai.aliyuncs.com/miyf/minio:latest` | `MIYF_IMAGE_MINIO` |
| `server` | `registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0` | `MIYF_IMAGE_SERVER` |
| `nginx` | `registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0` | `MIYF_IMAGE_NGINX` |

> 等价于：
>
> ```bash
> docker pull registry.cn-shanghai.aliyuncs.com/miyf/postgres:16-alpine
> docker pull registry.cn-shanghai.aliyuncs.com/miyf/redis:7-alpine
> docker pull registry.cn-shanghai.aliyuncs.com/miyf/minio:latest
> docker pull registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
> docker pull registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
> ```
>
> 须先 `docker login registry.cn-shanghai.aliyuncs.com`。  
> 上述仓库须已在 ACR 创建并 push（见 **4.0**）。业务镜像 tag 以 `.env` 为准。

```bash
cd /miyf/app/deploy
docker compose pull
docker compose up -d
docker compose ps
```

期望：`postgres` / `redis` / `minio` / `server` / `nginx` 均为 `running`（health 最终为 healthy）。  
`server` 首次启动含 Flyway，可能 1～2 分钟才 healthy。

**6）安装 Nginx 日志按日压缩（建议，见 1.5）**

```bash
sudo cp /miyf/app/deploy/logrotate/miyf /etc/logrotate.d/miyf
sudo logrotate -d /etc/logrotate.d/miyf
```

**7）快速自检**

```bash
curl -s http://127.0.0.1/healthz
curl -s http://127.0.0.1/actuator/health
curl -s http://47.108.200.201/healthz    # 公网（安全组已放行时）
docker compose logs --tail=80 server
ls -la /miyf/log/server /miyf/log/nginx
```

更完整验收见 **第 5 节**。默认管理员：`admin` / `change-me`（登录后立刻改密）。

#### 备用：docker load（未用 ACR pull 业务镜像时）

```bash
docker load -i /miyf/miyf-images.tar
cd /miyf/app/deploy
# .env 中 MIYF_IMAGE_* 须与 load 后的 tag 一致
docker compose pull postgres redis minio
docker compose up -d
```

#### 日常运维

```bash
cd /miyf/app/deploy

# 日志（容器 stdout；文件日志在 /miyf/log/...）
docker compose logs -f --tail=200 server
docker compose logs -f --tail=200 nginx

# 停止 / 启动 / 重启单个服务
docker compose stop
docker compose start
docker compose restart server

# 拆除容器与网络（不会删除 /miyf/data、/miyf/log 宿主机数据）
docker compose down
```

发版更新（改 tag、pull、up）见 **第 7 节**。

#### 启动失败时先查

| 现象 | 处理 |
|------|------|
| `pull` 业务镜像 401 / denied | 重新 `docker login` ACR；核对仓库权限与镜像名 |
| `pull` 业务镜像 not found | `.env` 的 tag 与 ACR 控制台不一致，或尚未 push |
| `postgres` Permission denied | `chown -R 70:70 /miyf/data/db/data` |
| `server` 写日志 Permission denied（`/miyf/log/server/miyf.log`） | 宿主机目录须属容器用户 uid **1000**：`sudo chown -R 1000:1000 /miyf/log/server && sudo chmod 755 /miyf/log/server`，再 `docker compose up -d server`。可用 `docker run --rm --entrypoint id <MIYF_IMAGE_SERVER>` 核对 uid |
| `server` 一直 unhealthy | `docker compose logs server`；查 DB 密码、Flyway、端口 |
| `nginx` 不起 | 依赖 `server` healthy；先修好 server |
| 页面 / API 公网不通 | 安全组 80、本机 `ufw`、`NGINX_HTTP_PORT` |
| 图片 404 | `FILE_STORAGE_BASE_URL` 是否为 `http://公网IP/media` |

---

## 5. 验收清单

把 `47.108.200.201` 换成你的 IP/域名后逐项检查：

| 检查项 | 命令 / 地址 | 期望 |
|--------|-------------|------|
| Nginx 存活 | `curl -s http://47.108.200.201/healthz` | `ok` |
| 应用健康 | `curl -s http://47.108.200.201/actuator/health` | 含 `"status":"UP"` |
| 管理后台 | 浏览器打开 http://47.108.200.201/ | 登录页 |
| API 文档 | http://47.108.200.201/swagger-ui/ | 可打开（生产可关） |
| 管理员登录 | 后台 `admin` / `change-me` | 进入工作台 |

路径一览：

| 路径 | 说明 |
|------|------|
| `/` | 管理后台（miyf-admin） |
| `/api/` | 后端 API |
| `/media/` | 上传文件公开读 |
| `/actuator/health` | 健康检查 |
| `/swagger-ui/` | OpenAPI UI |
| `/healthz` | Nginx 自身探针 |

容器状态：

```bash
docker compose ps
# postgres / redis / minio / server / nginx 均应为 running（healthy）
```

---

## 6. 小程序对接

当前使用 **HTTP**：`http://www.miyf.cn`（未启用 HTTPS）。

```bash
# miniapp/.env.production：
# TARO_APP_API_BASE=http://www.miyf.cn
cd miniapp
npm install
npm run build:weapp
```

**限制**：微信体验版/正式版只允许 **HTTPS** 合法域名；HTTP 仅适合开发者工具勾选「不校验合法域名」，或真机调试。上线体验版仍须按 §9 启用 HTTPS 后再改回 `https://www.miyf.cn`。

服务端：

1. `FILE_STORAGE_BASE_URL=http://www.miyf.cn/media`，改后 `docker compose up -d server`  
2. 关闭 Mock 时：`WX_AUTH_MOCK_ENABLED=false`，并配置 `WX_APP_ID` / `WX_APP_SECRET`  

---

## 7. 更新发布

发版时递增版本号（如 `0.1.0` → `0.1.1`），本机与服务器 `.env` 的 `MIYF_IMAGE_*` 同步修改。

```bash
# --- 本机 ---
cd deploy
# 编辑 .env：MIYF_IMAGE_SERVER / MIYF_IMAGE_NGINX 改为新 tag
docker compose -f docker-compose.yml -f docker-compose.build.yml build
docker login --username=peteroyu registry.cn-shanghai.aliyuncs.com
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.1
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.1

# --- 服务器 ---
cd /miyf/app/deploy
# 同样改 .env 中 MIYF_IMAGE_* 为新 tag
docker login --username=peteroyu registry.cn-shanghai.aliyuncs.com
docker compose pull
docker compose up -d
docker compose ps
docker compose logs -f --tail=100 server
```

Flyway 会在 `server` 启动时自动执行迁移（当前为 `V1` schema + `V2` 种子）。**未上线前可直接改 migration；一旦正式环境已执行过，则不要改已应用文件，应新增 `V3`/`V4`…**

仅更新管理后台时：本机只 build/push `miyf_nginx`，服务器改 tag 后 `docker compose pull nginx && docker compose up -d nginx`。

---

## 8. 数据备份与恢复（简要）

备份文件建议落到 `/miyf/data/backup`。

备份 PostgreSQL：

```bash
cd /miyf/app/deploy
docker compose exec -T postgres \
  pg_dump -U postgres miyf \
  > /miyf/data/backup/pg-$(date +%F).sql
```

恢复（会覆盖库内数据，慎用）：

```bash
cat /miyf/data/backup/pg-YYYY-MM-DD.sql | docker compose exec -T postgres \
  psql -U postgres -d miyf
```

MinIO 对象在宿主机目录 `/miyf/data/oss/resource`：整机迁移可直接打包该目录，或使用 `mc mirror`。

```bash
sudo tar -czf /miyf/data/backup/oss-$(date +%F).tgz -C /miyf/data/oss resource
```

---

## 9. HTTPS（www.miyf.cn）

微信小程序体验版/正式版 **必须 HTTPS**。按顺序做：

### 0. 前提

- DNS：`www.miyf.cn`（建议再加 `miyf.cn`）A 记录 → `47.108.200.201`
- 阿里云安全组放行 **443/TCP**（80 已开则保留）

### 1. 准备证书

证书放到服务器：

```bash
sudo mkdir -p /miyf/data/nginx/certs
# 上传后文件名必须是：
#   /miyf/data/nginx/certs/fullchain.pem   # 完整证书链
#   /miyf/data/nginx/certs/privkey.pem     # 私钥
sudo chmod 640 /miyf/data/nginx/certs/privkey.pem
```

可用阿里云/腾讯云 SSL、或 Let’s Encrypt（certbot）。域名须与证书 CN/SAN 一致。

### 2. 改 `docker-compose.yml`（nginx 段）

取消这三处注释：

```yaml
ports:
  - "${NGINX_HTTP_PORT:-80}:80"
  - "${NGINX_HTTPS_PORT:-443}:443"          # ← 打开
volumes:
  - ${MIYF_LOG_NGINX:-/miyf/log/nginx}:/var/log/nginx
  - ${MIYF_DATA_NGINX_CERTS:-/miyf/data/nginx/certs}:/etc/nginx/certs:ro   # ← 打开
  - ./nginx/conf.d/https.conf:/etc/nginx/conf.d/https.conf:ro              # ← 打开（用 https.conf，不是 template）
```

仓库已提供可直接用的 [`nginx/conf.d/https.conf`](./nginx/conf.d/https.conf)（`www.miyf.cn` / `miyf.cn`）。

把更新后的 `docker-compose.yml` 和 `nginx/conf.d/https.conf` 同步到服务器 `/miyf/app/deploy/`。

### 3. 改服务器 `/miyf/app/deploy/.env`

```bash
FILE_STORAGE_BASE_URL=https://www.miyf.cn/media
```

（本机 `deploy/.env` 已写成该值，服务器需同样改。）

### 4. 重启

```bash
cd /miyf/app/deploy
docker compose up -d nginx server
# 验证
curl -fsS https://www.miyf.cn/healthz          # 期望 ok
curl -fsS https://www.miyf.cn/actuator/health  # 期望含 UP
```

若 nginx 起不来：`docker compose logs nginx`（常见是证书路径/文件名不对）。

### 5. 微信与小程序

1. 公众平台 → 服务器域名：`https://www.miyf.cn`（request / uploadFile / downloadFile）
2. 小程序已按该域名打包则可直接上传体验版

**未上 HTTPS 前**：体验版无法稳定调 API；开发者工具可勾选「不校验合法域名」继续用 HTTP IP 调试。


---

## 10. 常见问题

**Q: server 报 `/miyf/log/server/miyf.log (Permission denied)`？**  
A: 挂载目录属主不是容器内 `miyf`（固定 **uid/gid 1000**）。执行：

```bash
sudo mkdir -p /miyf/log/server
sudo chown -R 1000:1000 /miyf/log/server
sudo chmod 755 /miyf/log/server
cd /miyf/app/deploy && docker compose up -d server
```

旧镜像若 uid 不是 1000，先 `docker run --rm --entrypoint id <镜像>` 看实际 uid，或重建/推送含 `adduser -u 1000` 的新版 `miyf_app`。

**Q: postgres 报 Permission denied / 无法写数据目录？**  
A: 确认已 `chown -R 70:70 /miyf/data/db/data`（官方 postgres alpine 使用 uid 70）。

**Q: `nginx` 一直起不来 / 依赖 server healthy？**  
A: 看 `docker compose logs server`。Flyway、DB 连不上或首次 JVM 启动慢时，`start_period` 内会重试。确认 `POSTGRES_PASSWORD` 与 compose 一致。

**Q: 管理后台空白或 404？**  
A: 确认服务器拉到的是含最新 `miyf-admin` 的 `MIYF_IMAGE_NGINX`，然后 `docker compose up -d nginx`。本机构建上下文须为仓库根目录且存在 `miyf-admin/`。

**Q: 服务器上执行 build 报错 / 不想在服务器编译？**  
A: 正常。生产只用 `docker compose pull && up -d`。构建请在本机：`docker compose -f docker-compose.yml -f docker-compose.build.yml build`。

**Q: 上传成功但图片打不开？**  
A: 核对 `FILE_STORAGE_BASE_URL` 是否为公网可访问的 `http(s)://主机/media`，且未写成 `:9000`。改 `.env` 后需 `docker compose up -d server` 使环境变量生效。确认 `/miyf/data/oss/resource` 可写。

**Q: 登录 401 / 权限不足？**  
A: 确认用种子账号 `admin` / `change-me`；超级管理员角色会绑定启动扫描出的权限组。改 JWT_SECRET 后旧 Token 全部失效，需重新登录。

**Q: 小程序请求失败？**  
A: 检查 `TARO_APP_API_BASE` 是否带 `/api`；HTTP IP 仅适合开发工具；正式须 HTTPS + 合法域名。

**Q: build 报加速器 `403 Forbidden`（如 `mirror.aliyuncs.com/.../library/maven`）？**  
A: 个人加速器常无法代理这类官方镜像。按 **4.0** 把 maven / eclipse-temurin / node / nginx 导入 ACR，`.env` 的 `BASE_*_IMAGE` 指向 `registry.cn-shanghai.aliyuncs.com/miyf/...`，再 build。

**Q: 华为 OAuth 回调失败 / redirect_uri 不匹配？**  
A: `HUAWEI_HEALTH_REDIRECT_URI` 须与华为开放平台填写的回调 URL **完全一致**（含协议、域名、路径 `/health/providers`）。改
`.env` 后执行 `docker compose up -d server`。管理端若 URL 带 `error=`，页面会提示拒绝原因。

**Q: push ACR 报 `unknown manifest class for application/vnd.oci.empty.v1+json`？**  
A: 新版 BuildKit 默认写入 attestation，ACR 不认。用已关闭 `provenance`/`sbom` 的 `docker-compose.build.yml` **重新 build**
后再 push；或：

```powershell
$env:BUILDX_NO_DEFAULT_ATTESTS="1"
docker compose -f docker-compose.yml -f docker-compose.build.yml build --no-cache
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_app:0.1.0
docker push registry.cn-shanghai.aliyuncs.com/miyf/miyf_nginx:0.1.0
```

**Q: 磁盘占用过大？**  
A: 清理悬空镜像：`docker system prune -f`（勿加 `-a` 除非确认可删未用镜像）。数据在 `/miyf/data`，日志在 `/miyf/log`，与镜像无关。

**Q: 在哪看应用 / Nginx 日志？**  
A: 宿主机：`/miyf/log/server/`（`miyf.log` 与按日 `.log.gz`）、`/miyf/log/nginx/`。也可 `docker compose logs -f server` / `nginx`。Nginx 按日压缩需已安装 **1.5** 的 logrotate。

---

## 11. 目录与相关文件

```text
/miyf/
├── app/                      # 部署代码（本仓库）
│   ├── deploy/               # compose 工作目录
│   ├── server/
│   ├── miyf-admin/
│   └── ...
├── data/
│   ├── db/data/              # PostgreSQL
│   ├── redis/data/           # Redis
│   ├── oss/resource/         # MinIO
│   ├── nginx/certs/          # HTTPS（可选）
│   └── backup/               # 手工备份
└── log/
    ├── server/               # Logback：miyf.log + *.log.gz
    └── nginx/                # access.log / error.log（+ logrotate）

deploy/                       # 仓库内路径 = /miyf/app/deploy
├── README.md                 # 本文档
├── .env.example              # 含 MIYF_DATA_* / MIYF_LOG_* / MIYF_IMAGE_*
├── .env                      # 实际配置（勿提交密钥到公开仓库）
├── docker-compose.yml        # 生产：仅 image，服务器 pull 启动
├── docker-compose.build.yml  # 本机/CI 构建覆盖层
├── docker-daemon-mirrors.example.json  # Docker Desktop 阿里云加速示例
├── logrotate/miyf            # 安装到 /etc/logrotate.d/miyf
├── .dockerignore
└── nginx/
    ├── Dockerfile
    ├── nginx.conf
    └── conf.d/
        ├── default.conf
        └── https.conf.template

server/Dockerfile             # 后端多阶段构建 → server-boot jar；LOG_PATH=/miyf/log/server
miyf-admin/                   # 管理后台源码（打进 nginx 镜像）
```

仓库总览与本地开发说明见根目录 [README.md](../README.md)。
