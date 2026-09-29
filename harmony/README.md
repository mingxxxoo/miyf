# miyf 鸿蒙客户端

Stage 模型 + ArkTS。对接现有 `server/` 个人端 API，登录走 `POST /api/auth/huawei-login`。

## 已落地范围

### 一期 · 食客主链路

- 华为账号登录（正式）+ 开发 Mock（食客 `mock:diner` / 厨师 `mock:cook`）
- 选择身份、邀请码加入厨房
- 首页推荐、菜品列表/详情（步进器）、本地预约草稿、提交/取消预约、我的与退出
- 视觉对齐 `design/harmony-app-design.html` Token（奶油底 / 灶橙 / 页签）

### 二期 · 厨师工作台

- 工作台首页：四格数据、宫格入口、待处理预约一键确认/驳回
- 菜品管理：新建、状态筛选、上架/下架、提交审核
- 处理预约：状态推进（待确认 → … → 完成）
- 厨房资料与邀请码复制
- 食客申请：通过 / 拒绝
- 从「我的 → 进入厨房工作台」进入；工作台无底部四栏，可切回食客

### 三期 · 健康与华为授权

- 健康总览：综合评分、需关注横幅、指标卡
- 指标详情：7/30/90 天、统计与近期样本
- 数据源：华为运动健康优先；系统浏览器授权 + OAuth 状态轮询；立即同步 / 解除授权
- 入口：首页服务切换胶囊、我的「胡闹健康」

### 四期 · AI 草稿

- 食客「说一句话点菜」半模态：`POST /api/ai/orders/draft`，未匹配列表必展示
- 预约页饮食参考卡：`POST /api/ai/orders/insight`，失败整卡不出现
- 厨师「用文字生成」：`POST /api/chef/ai/dishes/extract` → 勾选草稿 → 创建并提交审核（不上架）

### 五期 · 2×2 服务卡片

- Form Kit：`MiyfFormAbility` + `OrderStatusCard`（仅 2×2）
- 仅推送备餐中 / 可取餐一张预约；无进行中则空态文案
- 主进程 `FormCardUpdater` 写 Preferences 并 `updateForm`；预约列表/详情、厨师推进状态后刷新
- 点击卡片 `postCardAction` → `EntryAbility` → `OrderDetail`（冷启动经 `AppNav.pendingOrderId`）

## 联调

1. 服务端 `dev` profile 默认 `app.auth.huawei.mock-enabled=true`。
2. 客户端 `AppConfig.API_BASE` 指向可访问的后端；本机调试可改为 `http://<电脑局域网IP>:8080`。
3. DevEco Studio 打开 `harmony/`，使用 HarmonyOS NEXT SDK 编译运行。
4. 「开发登录 · 食客」提交 `mock:diner`；「开发登录 · 厨师」提交 `mock:cook`。
5. 正式华为登录需配置 `HUAWEI_ACCOUNT_CLIENT_ID` / `HUAWEI_ACCOUNT_CLIENT_SECRET`，并关闭 Mock。
6. 健康华为授权需服务端配置 `HUAWEI_HEALTH_*`；点「去授权」会打开系统浏览器，客户端轮询 `/api/health/providers/huawei/oauth/status`。
7. AI 需 `APP_AI_ENABLED=true` 与模型密钥；关闭时点菜/建菜会降级提示，主流程仍可手动操作。
8. 桌面长按添加「预约状态」服务卡片；需已登录且有备餐中/可取餐预约才会显示内容。

## 目录

```
entry/src/main/ets/
  api/          # 无 UI 的 HTTP 与 DTO，供以后安卓/iOS 对照路径
  platform/     # Account Kit、系统浏览器、服务卡片刷新
  session/      # token、预约草稿、底栏导航、卡片快照
  entryformability/  # FormExtensionAbility
  widget/pages/ # 2×2 OrderStatusCard
  pages/        # 食客页（含 OrderDetail）
  pages/chef/   # 厨师工作台与子页（含 AI 建菜）
  pages/health/ # 健康总览 / 详情 / 数据源
```

## 设计与开发提示词

- 鸿蒙完整设计：[`design/HARMONY_APP_DESIGN.md`](../design/HARMONY_APP_DESIGN.md)
- 鸿蒙高保真演示：[`design/harmony-app-design.html`](../design/harmony-app-design.html)
- 三端 UI / 分期提示词：[`design/NATIVE_CLIENT_PROMPTS.md`](../design/NATIVE_CLIENT_PROMPTS.md)
- 入口摘要：[`docs/NATIVE_CLIENT_PROMPTS.md`](../docs/NATIVE_CLIENT_PROMPTS.md)
- 小程序视觉基线（Token 同源）：[`design/miniapp-redesign.html`](../design/miniapp-redesign.html)
