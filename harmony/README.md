# miyf 鸿蒙客户端（第一期）

Stage 模型 + ArkTS。对接现有 `server/` 个人端 API，登录走 `POST /api/auth/huawei-login`。

## 本期内范围

- 华为账号登录（正式）+ 开发 Mock 登录（`AppConfig.DEV_LOGIN`）
- 选择食客身份、邀请码加入厨房
- 首页推荐、菜品列表/详情、本地预约草稿、提交/取消预约、我的与退出

未做：厨师工作台、健康、AI、服务卡片。

## 联调

1. 服务端 `dev` profile 默认 `app.auth.huawei.mock-enabled=true`。
2. 客户端 `AppConfig.API_BASE` 指向可访问的后端；本机调试可改为 `http://<电脑局域网IP>:8080`。
3. DevEco Studio 打开 `harmony/`，使用 HarmonyOS NEXT SDK 编译运行。
4. 点「开发登录（Mock）」会提交授权码 `mock:diner`，服务端映射为 `mock_union_diner`。
5. 正式华为登录需配置 `HUAWEI_ACCOUNT_CLIENT_ID` / `HUAWEI_ACCOUNT_CLIENT_SECRET`，并关闭 Mock。

## 目录

```
entry/src/main/ets/
  api/          # 无 UI 的 HTTP 与 DTO，供以后安卓/iOS 对照路径
  platform/     # Account Kit
  session/      # token、预约草稿
  pages/        # 页面
```

## 设计与开发提示词

三端 UI 与鸿蒙分期提示词见仓库根目录：

- [`design/NATIVE_CLIENT_PROMPTS.md`](../design/NATIVE_CLIENT_PROMPTS.md)
- 入口摘要：[`docs/NATIVE_CLIENT_PROMPTS.md`](../docs/NATIVE_CLIENT_PROMPTS.md)
- 小程序视觉基线：[`design/miniapp-redesign.html`](../design/miniapp-redesign.html)
