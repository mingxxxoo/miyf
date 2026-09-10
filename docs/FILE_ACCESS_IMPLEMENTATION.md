# 文件资源访问改造实施文档

## 1. 目标

将业务图片和附件从“业务表保存完整 URL”改为“业务表保存文件 ID”，并统一通过文件服务生成可访问地址。

目标架构：

```text
上传文件 -> sys_resource_index 保存元数据 -> 业务表只保存 fileId
        -> 统一文件查询接口按当前用户权限生成 url -> 前端使用 url 渲染图片
```

文件读取继续使用现有的短期签名 URL：`/r/{fileId}?exp={unix}&sig={hmac}`。

## 2. 现状与问题

当前文件服务已经具备以下能力：

- `sys_resource_index` 保存文件 ID、存储路径、MIME、创建人和访问权限。
- `GET /r/{fileId}` 支持 `exp/sig` 短期签名访问。
- `@FileAccess` 和 `FileAccessResponseAdvice` 可以将响应中的文件 URL 改写为签名 URL。
- 前端上传接口返回 `url`，业务模块通常直接保存该 URL。
- 菜品、评论等业务表仍使用 `cover_image`、`image_url` 等 URL 字段。

主要问题：

1. URL 中混入环境域名，迁移环境或修改域名后历史数据失效。
2. 签名 URL 有有效期，不能作为业务数据的永久值保存。
3. `<img>` 请求无法自动携带 Axios 的 Bearer Token。
4. 每个业务模块自行拼接 `/r/{id}`，权限和兼容逻辑容易分散。

## 3. 访问权限模型

| 权限 | 说明 | 默认使用场景 |
|---|---|---|
| `PUBLIC` | 无登录也可读取 | 公开菜品封面、公开宣传图 |
| `AUTHENTICATED` | 任意登录用户可读取 | 登录后可见的业务图片 |
| `OWNER` | 上传人或临时授权者可读取 | 用户私有图片、订单附件 |
| `ADMIN` | 仅管理员可读取 | 管理后台附件 |
| `DENY` | 禁止读取 | 删除、隔离或风控文件 |

约束：

- 默认上传权限使用 `OWNER`，禁止默认改为 `PUBLIC`。
- 签名 URL 只代表“在有效期内允许读取”，不改变资源本身的权限归属。
- 生产环境 `FILE_STORAGE_PUBLIC_READ=false`。

## 4. 数据模型改造

### 4.1 新增文件 ID 字段

新增迁移文件：

```text
server/server-boot/src/main/resources/db/migration/postgresql/V26__file_reference_ids.sql
```

建议字段：

```sql
ALTER TABLE users ADD COLUMN avatar_file_id BIGINT;
ALTER TABLE dish ADD COLUMN cover_file_id BIGINT;
ALTER TABLE dish_image ADD COLUMN file_id BIGINT;
ALTER TABLE comment_images ADD COLUMN file_id BIGINT;

CREATE INDEX idx_dish_cover_file_id ON dish(cover_file_id);
CREATE INDEX idx_dish_image_file_id ON dish_image(file_id);
CREATE INDEX idx_comment_image_file_id ON comment_images(file_id);
```

旧字段暂时保留，用于灰度期间兼容读取：`avatar_url`、`cover_image`、`image_url`。

### 4.2 数据迁移规则

历史 URL 只在可以解析出 `/r/{fileId}` 时回填：

```sql
UPDATE dish
SET cover_file_id = CAST(substring(cover_image FROM '/r/([0-9]+)') AS BIGINT)
WHERE cover_file_id IS NULL
  AND cover_image ~ '/r/[0-9]+';
```

无法解析的外部图片 URL 不转换，继续保留在旧字段中，后续由业务决定是否重新上传。

## 5. 后端接口设计

### 5.1 上传接口

保留现有接口：

```http
POST /api/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

返回值调整为：

```json
{
  "id": "123456789",
  "fileId": "123456789",
  "contentType": "image/png",
  "size": 102400,
  "appCode": "kitchen",
  "accessPermission": "OWNER",
  "url": "/r/123456789?exp=1789000000&sig=..."
}
```

说明：`fileId` 是业务保存值；`url` 只用于即时预览，不能作为业务表永久值；`path` 不应返回给前端。

### 5.2 批量文件信息接口

新增：

```http
GET /api/files?ids=123,456,789
Authorization: Bearer <token>
```

返回：

```json
{
  "code": 0,
  "data": [
    {
      "fileId": "123",
      "fileName": "cover.png",
      "mimeType": "image/png",
      "size": 102400,
      "url": "/r/123?exp=1789000000&sig=...",
      "accessPermission": "OWNER"
    }
  ]
}
```

接口规则：

- 最多允许 100 个 ID。
- 不存在或无权限的文件不返回详细信息，避免泄露资源是否存在。
- 对每个 ID 执行 `canAccess(fileId, READ)`。
- 签名 URL 有效期建议为 30 分钟。
- `PUBLIC` 文件可以返回无签名 URL；其他权限统一返回签名 URL。

### 5.3 单文件信息接口

新增：

```http
GET /api/files/{fileId}
Authorization: Bearer <token>
```

用于详情页、编辑页和需要展示文件名的组件。

### 5.4 文件读取接口

保留：

```http
GET /r/{fileId}?exp={unix}&sig={hmac}
```

读取逻辑：先校验签名；签名有效则读取文件；无签名时按登录主体、创建人、临时授权和资源权限判断。

HMAC 密钥：`FILE_STORAGE_ACCESS_SIGN_SECRET`（独立于 JWT）。

相关实现：

- `cn.miyf.oss.security.FileAccessSigner`
- `cn.miyf.oss.security.FileAccessResponseAdvice`
- `cn.miyf.oss.controller.FileResourceController`

## 6. 后端代码拆分

建议新增：

```text
server/lib-core/oss-service/src/main/java/cn/miyf/oss/
├── controller/FileQueryController.java
├── bean/vo/FileResourceVo.java
└── service/FileResourceQueryService.java
```

`FileResourceVo` 建议字段：

```java
private String fileId;
private String fileName;
private String mimeType;
private Long size;
private String url;
private FileAccessPermission accessPermission;
```

URL 生成逻辑集中在 `FileResourceQueryService`，业务服务不得自行调用 `sign()` 或拼接 `/r/`。

## 7. 业务模块改造

### 7.1 DTO/Entity

菜品示例：

```java
// Entity
private Long coverFileId;
private List<Long> imageFileIds;

// Save DTO
private Long coverFileId;
private List<Long> imageFileIds;
```

过渡期同时接收旧字段；当 `coverFileId` 为空时，尝试从旧 URL 解析文件 ID。

### 7.2 展示 VO

推荐返回文件对象：

```json
{
  "cover": {
    "fileId": "123",
    "url": "/r/123?exp=...&sig=...",
    "mimeType": "image/png"
  }
}
```

如果暂时不调整前端类型，可继续在字符串字段上使用 `@FileAccess`，但字段值必须由后端根据 `coverFileId` 动态转换，不能直接读取旧 URL。

### 7.3 业务查询性能

- 列表接口先批量收集所有 `fileId`。
- 一次查询 `sys_resource_index`，禁止在循环中逐条查询。
- 使用 Map 将文件信息回填到 VO。
- 签名 URL TTL 统一配置，不在业务代码中硬编码。

## 8. 前端改造

### 8.1 上传结果

```ts
export type UploadResult = {
  id: string;
  fileId: string;
  url: string;
  contentType?: string;
  size?: number;
};
```

上传成功后，表单保存 `fileId`，预览使用 `url`：

```ts
const result = await uploadFile(file, options);
onChange?.(result.fileId);
setPreviewUrl(result.url);
```

### 8.2 统一文件 API

新增 `miyf-admin/src/api/files.ts`：

```ts
export type FileResource = {
  fileId: string;
  fileName?: string;
  mimeType?: string;
  size?: number;
  url: string;
};

export function getFiles(ids: string[]): Promise<FileResource[]>;
export function getFile(fileId: string): Promise<FileResource>;
```

### 8.3 图片组件

新增统一组件，例如：

```tsx
<FileImage fileId={dish.coverFileId} />
```

组件行为：

1. 有当前接口返回的 `url` 时直接展示。
2. 只有 `fileId` 时调用批量文件接口。
3. URL 过期或加载失败时重新获取一次 URL。
4. 连续失败后显示占位图，不进入死循环。

业务页面不得直接拼接 `/r/{fileId}`。

## 9. 兼容与发布步骤

### 阶段一：后端兼容

- 增加 `fileId` 字段和索引。
- 新增 `/api/files` 查询接口。
- 上传接口同时返回 `id/fileId/url`。
- 业务查询同时支持旧 URL 和新 `fileId`。
- 保留 `@FileAccess` 签名机制。

### 阶段二：前端切换

- 上传表单保存 `fileId`。
- 图片组件统一从 `/api/files` 获取展示 URL。
- 详情页、列表页、编辑页逐步替换直接 URL 拼接。
- 对历史数据继续兼容旧 URL。

### 阶段三：数据回填

- 执行 V26 数据回填。
- 检查可解析旧 URL 是否全部完成转换。
- 统计无法转换的外部图片 URL。
- 观察线上图片读取错误率至少一个发布周期。

### 阶段四：清理旧字段

满足以下条件后再删除旧字段：业务代码不再写入旧 URL；数据库只剩明确的外部 URL；前端无直接 URL 拼接；灰度期间没有图片 404 或权限错误增长。

## 10. 安全要求

- JWT Secret 使用生产随机密钥，禁止默认值。
- 文件签名密钥使用独立配置 `FILE_STORAGE_ACCESS_SIGN_SECRET` / `app.file-storage.access-sign-secret`，**禁止复用 JWT_SECRET 或 MinIO secret-key**。
- 签名内容至少包含 `fileId + exp`（HMAC-SHA256）；篡改 exp/fileId 会使 sig 失效。
- `/r/**` 在 Spring Security 层 permitAll（img 无法带 Bearer），鉴权在应用层完成（签名 / PUBLIC / OWNER / Redis 临时权）。
- 签名 URL TTL 由系统配置 `file.access.sign.ttl.seconds` 热管理（默认 18000 秒），管理端「基础设置」可改，无需重启；缺失时回退 5 小时。
- 不在日志中打印完整签名 URL。
- 不向前端返回对象存储 Access Key、Secret Key 或内部路径。
- `OWNER` 文件必须校验创建人或临时访问授权（无有效签名时）。
- `DENY` 文件即使携带旧签名也不能访问；如需立即吊销，应增加资源版本或吊销时间校验。
- 限制批量查询 ID 数量，防止枚举文件资源。
- 生产/docker 启动由 `ProductionSecurityGuard` 拒绝弱密钥与默认值。

## 11. 测试与验收

### 后端测试

- 签名 URL 可以生成和校验。
- 过期签名、错误签名返回 `403`。
- `PUBLIC` 文件匿名可读。
- `OWNER` 文件非创建人不可读。
- `ADMIN` 文件普通用户不可读。
- `/api/files?ids=` 不泄露无权限文件元数据。
- 批量查询不会产生 N+1 查询。

### 前端测试

- 上传后页面立即显示图片。
- 刷新页面后通过 `fileId` 重新获取 URL 并显示。
- 签名 URL 过期后自动刷新一次。
- 图片加载失败显示占位图。
- 修改域名后历史数据仍可正常展示。
- 旧 URL 数据在迁移完成前仍可显示。

### 联调验收

```text
上传成功 -> 返回 fileId 和签名 url
保存业务数据 -> 只保存 fileId
查询业务详情 -> 返回签名 url
浏览器 img 请求 -> HTTP 200 + 正确 Content-Type
退出登录 -> PUBLIC 可见，OWNER/ADMIN 不可见
重新登录其他用户 -> 不可读取无权限文件
```

## 12. 推荐实施顺序

1. 先实现 `/api/files` 和 `FileResourceQueryService`。
2. 修改上传返回结构，保留旧字段兼容。
3. 先改菜品封面和菜品图片两个高频场景。
4. 再改用户头像、评论图片和订单附件。
5. 完成历史数据回填。
6. 最后删除前端直接拼接资源 URL 的代码。

最终原则：业务系统只管理 `fileId` 和业务权限，文件服务负责元数据、鉴权和签名 URL，前端只消费文件服务返回的展示地址。
