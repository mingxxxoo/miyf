<timestamp>Friday, Sep 4, 2026, 4:01 PM (UTC+8)</timestamp>
<user_query>
# clumsy_kitchen 全栈系统开发总提示词

你是一名资深的软件架构师、Java 后端工程师、React 前端工程师、微信小程序工程师、PostgreSQL 数据库工程师、DevOps 工程师。

你的任务是从 0 到 1 完整设计、开发、测试和交付一个真实可运行的项目：

# clumsy_kitchen

中文名称：

# 笨拙厨房

---

# 1. 项目目标

开发一个非商业化的厨房菜品预约与管理系统。

系统包含：

```text
1. 微信小程序
2. React Web 管理后台
3. Spring Boot 后端 API
4. PostgreSQL
5. Redis
6. Docker / Docker Compose
7. Nginx
8. Swagger / OpenAPI
9. 完整 README.md
10. 自动化测试
```

项目必须按照真实生产项目的方式进行设计。

不要只生成 Demo。

不要生成只能展示页面的假数据项目。

核心业务必须真正打通：

```text
管理员登录
    ↓
创建菜品分类
    ↓
创建菜品
    ↓
创建菜谱
    ↓
上传图片
    ↓
设置可提供数量
    ↓
菜品上架
    ↓
微信用户登录
    ↓
浏览菜品
    ↓
查看菜谱
    ↓
选择菜品
    ↓
选择数量
    ↓
提交预约
    ↓
厨房确认
    ↓
开始准备
    ↓
准备完成
    ↓
订单完成
    ↓
用户评价
    ↓
自动计算菜品评分
    ↓
管理员查看和管理评论
```

---

# 2. 非商业化业务边界

这是整个项目最重要的业务约束之一。

本项目：

> 不涉及任何金钱。

不允许自行加入以下功能、字段或概念：

```text
价格
金额
支付
付款
微信支付
支付宝
银行卡
余额
积分
优惠券
折扣
退款
收入
营业额
销售额
利润
GMV
客单价
交易流水
币种
支付状态
支付方式
```

本系统不是：

```text
电商系统
外卖平台
支付平台
商业订单平台
```

本系统是：

```text
菜品展示
+
菜谱管理
+
菜品预约
+
厨房处理
+
订单/预约状态管理
+
用户评价
+
后台管理
```

这里的“订单”仅表示：

> 用户向厨房提交的菜品预约/需求记录。

因此：

```text
订单 ≠ 商业交易
订单 ≠ 支付订单
订单 ≠ 金钱交易
```

如果未来需要商业化，应作为独立业务模块重新设计，不要在当前版本中提前加入。

---

# 3. 项目名称规范

整个项目统一使用：

```text
clumsy_kitchen
```

禁止继续使用：

```text
kitchen-system
```

Java 基础包名建议：

```text
com.clumsys.kitchen
```

前端项目名称：

```text
@clumsy-kitchen/miniapp
@clumsy-kitchen/admin-web
```

具体 package name 可根据实际 npm / Maven 命名规范调整，但整体命名需要保持一致。

---

# 4. 产品调性

产品核心感觉：

> “厨房里偶尔有点手忙脚乱，但认真做出来的饭很好吃。”

关键词：

```text
可爱
清爽
温暖
亲切
简单
生活化
轻松
有一点手作感
```

英文关键词：

```text
Cute
Clean
Fresh
Warm
Friendly
Minimal
Handmade
```

要求：

* 可爱但不幼稚
* 清爽但不冷淡
* 有生活感但不凌乱
* 有品牌个性但不影响使用效率

---

# 5. UI 视觉设计

## 5.1 颜色

主色建议：

```text
#FFB36B
```

柔和杏橙 / 奶油橙。

辅助色：

```text
#9DD9C4
```

薄荷绿。

背景：

```text
#FFFDF8
```

奶油白。

主文字：

```text
#333333
```

辅助文字：

```text
#888888
```

边框：

```text
#F0ECE5
```

整体使用：

> 低饱和、柔和、温暖的配色。

禁止高饱和霓虹风。

---

# 6. UI 圆角

建议：

```text
卡片：16px
按钮：12px
输入框：12px
图片：14px
弹窗：18px
```

视觉整体偏圆润。

---

# 7. UI 装饰

可以少量使用：

```text
小锅
小碗
勺子
小厨师
蒸汽
小火苗
星星
爱心
```

但必须遵守：

> 装饰服务于内容，不能喧宾夺主。

不要做成儿童教育 App。

不要大量使用 Emoji。

不要让整个页面充满卡通插画。

---

# 8. 小程序技术栈

微信小程序必须使用：

```text
Taro
React
TypeScript
Zustand
SCSS
```

禁止使用：

```text
Vue
uni-app
```

必须使用 React 函数组件和 Hooks。

禁止大量使用：

```typescript
any
```

所有核心业务模型必须定义 TypeScript 类型。

---

# 9. 管理后台技术栈

使用：

```text
React
TypeScript
Vite
React Router
Zustand
Ant Design
Axios
ECharts
```

管理后台和微信小程序共用后端 API。

---

# 10. 后端技术栈

必须使用：

```text
JDK 21
Spring Boot 4.x
Spring Web
Spring Security
Spring Validation
MyBatis-Plus
PostgreSQL
Redis
JWT
Druid
Lombok
Jackson
Maven
OpenAPI / Swagger
Docker
```

Spring Boot 使用开发时可获得的稳定版本。

不要为了“最新版”使用：

```text
SNAPSHOT
M1
M2
RC
Beta
```

等预发布版本。

最终 README 必须记录项目实际使用的版本号。

---

# 11. 数据库

当前默认数据库：

```text
PostgreSQL
```

但是 PostgreSQL 不是整个系统的永久架构绑定。

必须设计成：

> 当前默认 PostgreSQL，未来可切换或增加其他关系型数据库。

未来可能支持：

```text
PostgreSQL
MySQL
MariaDB
Oracle
SQL Server
```

当前版本只要求完整实现 PostgreSQL。

---

# 12. 数据库可替换架构

核心业务层不得直接依赖某一种数据库。

架构：

```text
Controller
    ↓
Application / Service
    ↓
Domain Repository
    ↓
Infrastructure
    ↓
DataSource
    ↓
具体数据库
```

推荐结构：

```text
server/src/main/java/com/clumsys/kitchen/

├── common/
├── config/
├── controller/
├── application/
├── domain/
│   ├── model/
│   ├── service/
│   └── repository/
├── infrastructure/
│   ├── persistence/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── repository/
│   └── datasource/
├── security/
├── dto/
├── vo/
├── exception/
└── ClumsyKitchenApplication.java
```

---

# 13. Repository 抽象

核心业务 Service 依赖 Repository 接口。

例如：

```java
public interface DishRepository {

    Dish findById(UUID id);

    PageResult<Dish> findPage(DishQuery query);

    void save(Dish dish);

    void update(Dish dish);
}
```

业务层：

```java
@Service
public class DishService {

    private final DishRepository dishRepository;

}
```

禁止：

```java
@Service
public class DishService {

    private final PostgreSqlDishMapper mapper;

}
```

业务 Service 不得直接感知 PostgreSQL。

---

# 14. 数据库厂商差异隔离

如果未来不同数据库存在：

```text
SQL 差异
分页差异
JSON 差异
UUID 差异
日期函数差异
UPSERT 差异
```

必须放在 Infrastructure / Data Access 层。

禁止把：

```text
PostgreSQL 特有 SQL
MySQL 特有 SQL
Oracle 特有 SQL
```

直接写入 Domain / Service。

---

# 15. 数据源设计

系统必须预留多数据源能力。

默认：

```text
PRIMARY
```

未来可以：

```text
SECONDARY
REPORTING
```

等。

架构：

```text
                    Service
                       ↓
                   Repository
                       ↓
                 DataSourceRouter
                       ↓
          ┌────────────┴────────────┐
          ↓                         ↓
       PRIMARY                  SECONDARY
          ↓                         ↓
   DruidDataSource           DruidDataSource
          ↓                         ↓
      PostgreSQL                 MySQL
```

---

# 16. 数据库连接池

数据库连接池必须使用：

# Druid

禁止使用：

```text
HikariCP
Tomcat JDBC Pool
其他连接池
```

如果 Spring Boot 默认带入 HikariCP，必须进行合理配置或排除，确保实际 DataSource 为 Druid。

---

# 17. Druid 数据源

每个数据源独立使用：

```text
DruidDataSource
```

不能多个数据库共用同一个连接池。

配置建议：

```yaml
datasource:
  default: primary

  sources:

    primary:
      enabled: true
      type: postgresql
      url: ${PRIMARY_DB_URL}
      username: ${PRIMARY_DB_USERNAME}
      password: ${PRIMARY_DB_PASSWORD}

      druid:
        initial-size: 5
        min-idle: 5
        max-active: 20
        max-wait: 10000
        validation-query: SELECT 1
        test-while-idle: true
        test-on-borrow: false
        test-on-return: false
        time-between-eviction-runs-millis: 60000
        min-evictable-idle-time-millis: 300000

    secondary:
      enabled: false
      type: mysql
      url: ${SECONDARY_DB_URL}
      username: ${SECONDARY_DB_USERNAME}
      password: ${SECONDARY_DB_PASSWORD}
```

参数只能作为参考。

需要根据实际：

```text
数据库最大连接数
服务器 CPU
服务实例数量
实际并发量
```

进行调整。

---

# 18. Druid 工厂

建议设计：

```text
infrastructure/datasource/

DataSourceFactory
DataSourceProperties
DataSourceRegistry
DataSourceRouter
DatabaseType
DataSourceKey
```

负责：

```text
读取配置
↓
创建 DruidDataSource
↓
注册数据源
↓
路由数据源
↓
生命周期管理
```

---

# 19. 数据库类型

例如：

```java
public enum DatabaseType {

    POSTGRESQL,
    MYSQL,
    MARIADB,
    ORACLE,
    SQLSERVER
}
```

当前：

```text
POSTGRESQL
```

未来增加数据库时：

> 不应该大范围修改 Domain 和 Service。

---

# 20. Druid 监控

开启必要的：

```text
SQL 统计
连接池状态
活跃连接
空闲连接
最大连接数
SQL 耗时
```

可以提供：

```text
/druid/*
```

Druid StatView。

生产环境必须：

```text
启用认证
限制访问
禁止匿名访问
```

账号密码必须来自环境变量。

---

# 21. Druid 安全

环境变量：

```env
DRUID_STAT_USERNAME=change-me
DRUID_STAT_PASSWORD=change-me
```

禁止：

* 默认弱密码
* 明文硬编码
* 提交 Git
* 生产环境开放公网匿名访问

生产环境 SQL 监控不能泄露：

```text
密码
Token
AppSecret
隐私参数
```

---

# 22. Redis

Redis 用于：

```text
缓存
登录辅助
热门菜品缓存
分类缓存
限流
分布式锁
```

不要把全部数据库查询都缓存。

必须考虑：

```text
缓存失效
缓存更新
缓存穿透
缓存雪崩
```

---

# 23. 数据库类型与 JSON

PostgreSQL 可以使用：

```text
JSONB
```

例如：

```text
ingredients
seasonings
steps
nutrition
```

但是：

> 不允许将整个业务模型设计成 JSONB。

核心字段必须结构化。

Domain 层使用自己的 Java 对象，而不是：

```text
PGobject
```

禁止 Domain 层直接依赖：

```text
org.postgresql.*
```

如果确实使用 PostgreSQL 特有类型：

> 必须限制在 Infrastructure 层。

---

# 24. 时间类型

业务层优先使用：

```text
Instant
LocalDate
LocalDateTime
```

推荐系统统一时区策略。

数据库当前 PostgreSQL 推荐：

```text
TIMESTAMPTZ
```

README 中明确：

```text
数据库时间策略
服务器时区
前端显示时区
```

---

# 25. ID

推荐统一使用：

```text
UUID
```

Domain 层不依赖 PostgreSQL UUID 专有实现。

未来如果 MySQL 使用：

```text
CHAR(36)
BINARY(16)
```

等，也由 Infrastructure 层处理。

---

# 26. Flyway

数据库迁移使用：

```text
Flyway
```

推荐：

```text
db/
├── migration/
│   ├── common/
│   └── postgresql/
```

如果 SQL 完全兼容多数据库，可放公共目录。

如果使用数据库专属 SQL，则必须：

```text
隔离
说明
测试
```

---

# 27. 用户角色

系统至少：

```text
USER
ADMIN
SUPER_ADMIN
```

---

# 28. USER 权限

普通用户可以：

```text
微信登录
浏览菜品
搜索菜品
查看分类
查看菜品详情
查看菜谱
创建预约
查看自己的预约
查看预约详情
取消符合条件的预约
查看自己的评论
发表评论
删除自己的评论
查看个人信息
```

普通用户不能：

```text
修改菜品
修改菜谱
修改库存
修改其他用户订单
查看管理员数据
管理评论
管理用户
访问管理员接口
```

---

# 29. ADMIN 权限

管理员：

```text
管理用户
管理分类
管理菜品
管理菜谱
管理预约单
管理评论
查看 Dashboard
```

---

# 30. SUPER_ADMIN 权限

超级管理员额外：

```text
管理员管理
角色管理
权限管理
操作日志
```

---

# 31. RBAC

必须使用标准 RBAC。

权限示例：

```text
dish:list
dish:create
dish:update
dish:delete
dish:publish
dish:unpublish

category:list
category:create
category:update
category:delete

recipe:list
recipe:create
recipe:update
recipe:delete

order:list
order:detail
order:update
order:cancel
order:complete

comment:list
comment:hide
comment:restore
comment:delete

user:list
user:detail
user:disable
user:enable

admin:list
admin:create
admin:update
admin:delete

role:list
role:create
role:update
role:delete
```

禁止使用：

```java
if ("admin".equals(username))
```

之类的硬编码权限。

---

# 32. 微信登录

使用：

```text
Taro.login()
```

流程：

```text
微信小程序
↓
获取 code
↓
POST /api/auth/wx-login
↓
后端调用微信接口
↓
获取 openid / unionid
↓
查询用户
↓
不存在则创建
↓
生成 JWT
↓
返回 Token
```

微信：

```text
WX_APP_ID
WX_APP_SECRET
```

只能配置在服务端。

禁止出现在：

```text
React
Taro
前端配置
Git
```

---

# 33. 管理员登录

使用：

```text
账号 + 密码
```

接口：

```http
POST /api/admin/auth/login
```

密码：

```text
BCrypt
```

禁止明文密码。

---

# 34. 菜品分类

表：

```text
dish_category
```

字段：

```text
id
name
icon
sort
status
created_at
updated_at
deleted
```

支持：

```text
新增
修改
删除
启用
禁用
排序
```

---

# 35. 菜品

字段：

```text
id
category_id
name
subtitle
description
cover_image
status
sort
is_recommend
stock
stock_type
unit
rating
rating_count
created_by
updated_by
created_at
updated_at
deleted
```

注意：

> 不允许有任何 price / amount 等金额字段。

---

# 36. 菜品状态

```text
DRAFT
ON_SALE
OFF_SALE
```

含义：

```text
DRAFT
草稿

ON_SALE
可预约

OFF_SALE
暂停预约
```

---

# 37. 菜品管理

管理员可以：

```text
新增
修改
删除
上架
下架
设置可提供份数
设置不限量
设置排序
设置推荐
上传封面
上传菜品图
```

历史订单使用过的菜品：

> 只能逻辑删除。

---

# 38. 菜品库存 / 可提供份数

这里的 stock：

> 表示厨房当前可以提供多少份。

不是商业商品库存。

例如：

```text
红烧鸡腿
今日可提供：10 份
```

预约：

```text
2 份
```

之后：

```text
剩余：8 份
```

---

# 39. 无限量模式

支持：

```text
LIMITED
UNLIMITED
```

如果：

```text
UNLIMITED
```

则：

```text
stock
```

不参与扣减。

前端显示：

```text
今日不限量
```

---

# 40. 菜谱

每个菜品可以拥有一个完整菜谱。

字段：

```text
id
dish_id
description
difficulty
prepare_minutes
cook_minutes
servings
ingredients
seasonings
steps
tips
nutrition
created_at
updated_at
```

---

# 41. 食材

结构：

```json
[
  {
    "name": "鸡腿",
    "amount": "2只"
  }
]
```

---

# 42. 调味料

结构：

```json
[
  {
    "name": "生抽",
    "amount": "2勺"
  }
]
```

---

# 43. 制作步骤

结构：

```json
[
  {
    "step": 1,
    "title": "准备食材",
    "description": "将鸡腿洗净备用",
    "image": "https://example.com/1.jpg"
  },
  {
    "step": 2,
    "title": "开始烹饪",
    "description": "锅中加入适量食用油",
    "image": "https://example.com/2.jpg"
  }
]
```

---

# 44. 菜谱后台

管理员可以：

```text
创建菜谱
修改菜谱
删除菜谱
编辑食材
编辑调味料
编辑制作步骤
上传步骤图片
设置难度
设置准备时间
设置烹饪时间
```

---

# 45. 预约单模型

系统中的订单实际上是：

```text
菜品预约单
```

代码层面可以：

```text
Order
OrderItem
```

但 UI 文案需要优先使用：

```text
预约
预约单
提交预约
厨房收到啦
```

而不是商业平台语言。

---

# 46. 订单表

```text
orders
```

字段：

```text
id
order_no
user_id
status
remark
created_at
updated_at
```

禁止：

```text
price
amount
total_amount
payment_status
payment_method
transaction_id
currency
refund
```

---

# 47. 订单明细

```text
order_items
```

字段：

```text
id
order_id
dish_id
dish_name
quantity
unit
remark
created_at
```

必须保存预约时的：

```text
dish_name
```

快照。

---

# 48. 订单状态

使用：

```text
PENDING
CONFIRMED
PREPARING
READY
COMPLETED
CANCELLED
```

正常流程：

```text
PENDING
↓
CONFIRMED
↓
PREPARING
↓
READY
↓
COMPLETED
```

取消：

```text
PENDING
↓
CANCELLED
```

其他状态是否允许取消，由业务规则明确限制。

禁止任意状态互相切换。

---

# 49. 创建预约

流程：

```text
用户选择菜品
↓
选择数量
↓
填写备注
↓
提交预约
```

示例：

```text
红烧鸡腿 × 2

备注：
少放辣椒
```

成功：

```text
🎉 厨房收到啦
```

---

# 50. 库存并发控制

如果：

```text
stock_type = LIMITED
```

创建预约必须：

```text
事务
+
库存校验
+
原子扣减
```

禁止：

```text
库存 1
用户 A 同时预约 1
用户 B 同时预约 1
最终库存 -1
```

可以使用：

```text
UPDATE ... WHERE stock >= quantity
```

结合事务。

或者：

```text
SELECT ... FOR UPDATE
```

两种方案选择一种合理实现。

---

# 51. 订单事务

创建预约必须保证：

```text
开始事务
↓
验证菜品
↓
验证上架状态
↓
验证库存
↓
扣减库存
↓
创建 orders
↓
创建 order_items
↓
提交事务
```

任何一步失败：

```text
ROLLBACK
```

---

# 52. 用户评价系统

用户可以对菜品进行：

```text
1 星
2 星
3 星
4 星
5 星
```

最高：

```text
★★★★★
```

最低：

```text
★☆☆☆☆
```

禁止：

```text
0 星
6 星
其他数字
```

---

# 53. 评价必须来自真实预约

用户只有完成真实预约后才能评价。

必须满足：

```text
订单属于当前用户
↓
订单状态 = COMPLETED
↓
订单中包含该菜品
↓
才能评价
```

后端必须执行校验。

前端隐藏按钮不能替代后端校验。

---

# 54. 一单一菜只能评价一次

例如：

```text
订单 A
├── 红烧鸡腿 × 2
└── 番茄炒蛋 × 1
```

用户可以分别评价：

```text
红烧鸡腿
番茄炒蛋
```

但：

```text
同一个订单
+
同一个菜品
+
同一个用户
```

只能评价一次。

数据库建立：

```sql
UNIQUE(order_id, dish_id, user_id)
```

---

# 55. 评论表

```text
comments
```

字段：

```text
id
user_id
dish_id
order_id
rating
content
status
created_at
updated_at
deleted
```

数据库：

```sql
CHECK (rating >= 1 AND rating <= 5)
```

---

# 56. 评论内容

规则：

```text
rating 必填
content 可选
```

因此允许：

```text
★★★★★
```

直接提交。

---

# 57. 菜品平均星级

每个菜品自动维护：

```text
rating
rating_count
```

例如：

```text
rating = 4.83
rating_count = 128
```

不能由管理员手工设置。

---

# 58. 评分计算

例如：

```text
5
4
5
3
5
```

平均：

```text
4.40
```

展示：

```text
★★★★★ 4.4
```

建议：

```text
NUMERIC(3,2)
```

保存。

---

# 59. 有效评论

只有：

```text
status = NORMAL
deleted = false
```

参与评分。

隐藏或删除：

```text
不参与评分
不展示给普通用户
```

---

# 60. 评论变化后自动重新计算

以下操作后：

```text
新增评论
修改评论
隐藏评论
恢复评论
删除评论
```

都必须重新计算：

```text
dish.rating
dish.rating_count
```

---

# 61. 没有评价

如果：

```text
rating_count = 0
```

显示：

```text
暂无评分
```

而不是：

```text
0 星
0.0
```

因为 0 星不是合法评价。

---

# 62. 评论筛选

支持：

```text
全部
5星
4星
3星
2星
1星
```

排序：

```text
最新
评分最高
评分最低
```

---

# 63. 评分组件

封装：

```text
StarRating
```

支持：

```text
readonly
editable
```

只读：

```text
★★★★★ 4.8
```

编辑：

```text
★☆☆☆☆
★★☆☆☆
★★★☆☆
★★★★☆
★★★★★
```

---

# 64. 管理员评论

管理员可以：

```text
查看
隐藏
恢复
删除
```

隐藏的评论：

```text
普通用户不可见
不参与评分
```

---

# 65. 菜品评分展示

小程序菜品卡：

```text
红烧鸡腿

★★★★★ 4.8

128 人评价

今日还有 8 份
```

菜品详情：

```text
★★★★★

4.8

128 人评价
```

---

# 66. 首页热门菜品

第一版可按：

```text
rating DESC
rating_count DESC
```

后续可以增加：

```text
预约次数
推荐权重
近期活跃度
评分
评价数量
```

禁止增加：

```text
销售额
金额
收入
```

等指标。

---

# 67. 微信小程序页面

至少：

```text
/pages/index/index
/pages/category/index
/pages/dish/detail
/pages/order/index
/pages/order/detail
/pages/comment/create
/pages/user/index
```

---

# 68. 首页布局

建议：

```text
品牌区域
↓
今日推荐
↓
菜品分类
↓
热门菜品
↓
今日可提供
↓
底部导航
```

---

# 69. 首页视觉

示意：

```text
┌────────────────────────┐
│ 🍳 clumsy_kitchen      │
│ 今天吃点什么呢？        │
├────────────────────────┤
│                        │
│ 今日推荐               │
│ ┌────────────────────┐ │
│ │                    │ │
│ │      菜品大图       │ │
│ │                    │ │
│ └────────────────────┘ │
│                        │
│ 分类                   │
│ 🍚  🥘  🍲  🥗  🍜    │
│                        │
│ 热门菜品               │
│ ┌────────┐ ┌────────┐ │
│ │  菜品  │ │  菜品  │ │
│ │ ★4.8  │ │ ★4.6  │ │
│ └────────┘ └────────┘ │
├────────────────────────┤
│ 首页  菜品  预约  我的 │
└────────────────────────┘
```

---

# 70. 菜品详情页

包含：

```text
菜品图片
菜品名称
简介
今日可提供数量
平均评分
评价数量
菜谱
食材
调味料
制作步骤
注意事项
评论
```

底部：

```text
选择数量
预约这道菜
```

禁止出现：

```text
购买
立即购买
支付
结算
加入购物车
```

---

# 71. 预约确认页

显示：

```text
菜品
数量
备注
```

按钮：

```text
提交预约
```

提交成功：

```text
🎉 厨房收到啦！
```

---

# 72. 用户订单页面

建议显示：

```text
全部
待确认
准备中
待完成
已完成
已取消
```

订单卡：

```text
订单编号
菜品
数量
备注
当前状态
提交时间
```

绝对不出现金额。

---

# 73. 用户个人中心

显示：

```text
头像
昵称
我的预约
我的评价
累计预约次数
完成次数
评价次数
```

不显示：

```text
消费金额
账户余额
积分
```

---

# 74. 管理后台

页面：

```text
/login

/dashboard

/users
/users/:id

/categories

/dishes
/dishes/create
/dishes/:id/edit

/recipes

/orders
/orders/:id

/comments

/admins

/roles

/permissions

/operation-logs
```

---

# 75. Dashboard

显示：

```text
今日预约数
待确认
准备中
待完成
今日完成
活跃用户
上架菜品
评价数量
```

图表：

```text
近 7 天预约趋势
热门菜品
菜品评分分布
用户活跃情况
```

禁止：

```text
销售额
营业额
收入
利润
GMV
客单价
```

---

# 76. 管理后台菜品列表

显示：

```text
菜品名称
分类
状态
预约状态
平均评分
评价数量
推荐
更新时间
操作
```

示例：

```text
红烧鸡腿
上架
★★★★★ 4.8
128 人评价
推荐
```

管理员不能修改：

```text
rating
rating_count
```

---

# 77. 操作日志

记录：

```text
管理员登录
创建菜品
修改菜品
删除菜品
上架
下架
修改菜谱
修改订单
隐藏评论
恢复评论
删除评论
用户禁用
角色变更
权限变更
```

表：

```text
operation_logs
```

建议：

```text
id
operator_id
operation_type
target_type
target_id
request_ip
request_method
request_uri
operation_detail
created_at
```

禁止记录：

```text
密码
Token
AppSecret
```

---

# 78. 文件上传

支持：

```text
菜品封面
菜品图片
菜谱步骤图片
评论图片
```

数据库只保存 URL。

设计：

```java
FileStorageService
```

开发环境：

```text
LocalFileStorageService
```

生产环境未来可以：

```text
MinIO
S3
OSS
COS
```

---

# 79. API 统一规范

前缀：

```text
/api
```

用户 API：

```http
POST /api/auth/wx-login

GET  /api/categories

GET  /api/dishes
GET  /api/dishes/{id}

POST /api/orders
GET  /api/orders
GET  /api/orders/{id}
POST /api/orders/{id}/cancel

POST /api/comments
GET  /api/dishes/{id}/comments
```

管理员：

```http
POST   /api/admin/auth/login

GET    /api/admin/dishes
POST   /api/admin/dishes
PUT    /api/admin/dishes/{id}
DELETE /api/admin/dishes/{id}

POST   /api/admin/dishes/{id}/publish
POST   /api/admin/dishes/{id}/unpublish

GET    /api/admin/recipes
POST   /api/admin/recipes
PUT    /api/admin/recipes/{id}
DELETE /api/admin/recipes/{id}

GET    /api/admin/orders
PUT    /api/admin/orders/{id}/status

GET    /api/admin/users
PUT    /api/admin/users/{id}/status

GET    /api/admin/comments
POST   /api/admin/comments/{id}/hide
POST   /api/admin/comments/{id}/restore
DELETE /api/admin/comments/{id}
```

实际 API 可以根据 RESTful 设计进一步调整，但必须保持统一。

---

# 80. 统一响应

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 40001,
  "message": "参数错误",
  "data": null
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

---

# 81. 全局异常

使用：

```java
@RestControllerAdvice
```

统一处理：

```text
参数异常
业务异常
认证异常
权限异常
数据库异常
系统异常
```

禁止向前端返回：

```text
StackTrace
SQL
数据库密码
内部实现细节
```

---

# 82. 安全

必须使用：

```text
Spring Security
JWT
BCrypt
RBAC
参数校验
SQL 注入防护
XSS 防护
文件上传校验
权限校验
越权校验
接口限流
```

用户：

> 只能访问自己的个人数据。

例如：

```text
自己的预约
自己的评价
自己的个人资料
```

管理员接口必须完整进行权限检查。

---

# 83. 前端 API 封装

微信小程序：

```text
src/api/
```

提供统一：

```typescript
request()
get()
post()
put()
delete()
```

自动处理：

```text
Token
业务错误
HTTP 错误
登录过期
```

管理后台：

```text
Axios
```

统一拦截器。

禁止每个页面自己实现网络请求处理。

---

# 84. Zustand

小程序：

```text
userStore
orderStore
```

如果有本地选择状态，可以：

```text
selectionStore
```

后台：

```text
authStore
permissionStore
```

不要滥用全局 Store。

---

# 85. 数据库表

至少：

```text
users

roles
permissions
user_roles
role_permissions

dish_category
dish
dish_image
dish_recipe

orders
order_items

comments
comment_images

admin_users
operation_logs
```

可以根据实际设计合并或拆分，但必须保持职责清晰。

---

# 86. 数据库索引

至少考虑：

```text
users.openid

dish.category_id
dish.status
dish.sort
dish.is_recommend

orders.user_id
orders.order_no
orders.status
orders.created_at

order_items.order_id
order_items.dish_id

comments.dish_id
comments.user_id
comments.order_id
comments.status
```

根据实际查询计划优化。

不要为所有字段建立索引。

---

# 87. 数据库约束

关键规则必须由数据库保证。

例如：

```sql
CHECK (rating >= 1 AND rating <= 5)
```

唯一：

```sql
UNIQUE(order_id, dish_id, user_id)
```

订单号：

```text
UNIQUE(order_no)
```

关键外键必须建立 Foreign Key。

---

# 88. 数据库事务

必须使用事务处理：

```text
创建预约
扣减库存
取消预约
修改预约状态
创建评价
重新计算评分
```

尤其：

```text
创建订单
+
库存扣减
```

必须保证事务一致性。

---

# 89. 数据源事务

当前：

```text
单数据源
```

普通：

```java
@Transactional
```

即可。

如果未来：

```text
PRIMARY
+
SECONDARY
```

共同参与一个业务事务：

不要认为普通 `@Transactional` 自动提供跨数据库事务。

未来需要跨库事务时，再根据实际业务选择：

```text
最终一致性
Outbox
消息队列
分布式事务
```

当前版本不要为了预留而引入复杂分布式事务。

---

# 90. 数据库切换

当前：

```env
DATABASE_TYPE=postgresql
```

以后可以：

```env
DATABASE_TYPE=mysql
```

架构目标：

> 修改数据源配置和对应 Infrastructure 实现后，Domain / Service 尽量无需修改。

---

# 91. 环境变量

必须生成：

```text
.env.example
```

例如：

```env
DATABASE_TYPE=postgresql

PRIMARY_DB_URL=jdbc:postgresql://localhost:5432/clumsy_kitchen
PRIMARY_DB_USERNAME=postgres
PRIMARY_DB_PASSWORD=change-me

SECONDARY_DB_ENABLED=false
SECONDARY_DB_TYPE=mysql
SECONDARY_DB_URL=
SECONDARY_DB_USERNAME=
SECONDARY_DB_PASSWORD=

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=change-me

WX_APP_ID=your-app-id
WX_APP_SECRET=your-app-secret

DRUID_STAT_USERNAME=change-me
DRUID_STAT_PASSWORD=change-me

FILE_STORAGE_TYPE=local
FILE_STORAGE_PATH=./uploads
```

真实密钥不得提交 Git。

---

# 92. 项目目录

最终：

```text
clumsy_kitchen/
│
├── server/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── miniapp/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── admin-web/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── sql/
│   ├── schema.sql
│   ├── data.sql
│   └── migration/
│
├── deploy/
│   ├── docker/
│   ├── nginx/
│   └── docker-compose.yml
│
├── docs/
│   ├── api/
│   ├── database/
│   └── architecture/
│
├── .env.example
├── .gitignore
└── README.md
```

---

# 93. 后端目录

建议：

```text
server/src/main/java/com/clumsys/kitchen/

├── common/
│   ├── api/
│   ├── constants/
│   ├── enums/
│   ├── utils/
│   └── result/
│
├── config/
│   ├── SecurityConfig
│   ├── RedisConfig
│   ├── MybatisConfig
│   ├── OpenApiConfig
│   └── ...
│
├── controller/
│
├── application/
│   ├── service/
│   └── ...
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
│
├── infrastructure/
│   ├── persistence/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── datasource/
│   └── storage/
│
├── security/
│
├── dto/
│
├── vo/
│
├── exception/
│
└── ClumsyKitchenApplication.java
```

---

# 94. DTO / VO / Entity

DTO：

> API 请求参数。

VO：

> API 返回结果。

Entity：

> 数据库对象。

Domain Model：

> 业务模型。

尽量避免直接：

```text
Entity → API Response
```

所有重要接口都使用 DTO / VO。

---

# 95. 订单业务校验

用户创建预约必须验证：

```text
用户是否登录
菜品是否存在
菜品是否已删除
菜品是否上架
库存是否足够
数量是否合法
```

禁止仅依赖前端传参。

---

# 96. 菜品下架规则

下架：

```text
OFF_SALE
```

之后：

```text
历史订单仍然可以查看
历史评论仍然可以查看
新用户不能创建预约
```

---

# 97. 用户评论校验

发表评论必须验证：

```text
用户身份
订单归属
订单完成状态
订单是否包含该菜
是否已经评价
rating 是否在 1~5
```

---

# 98. 菜品评分一致性

评分统计必须保证：

```text
dish.rating
dish.rating_count
```

与有效评论一致。

如果发现缓存或统计字段异常，提供管理员或内部任务重新计算功能：

```text
rebuildDishRating
```

重新根据评论计算。

---

# 99. 推荐的后台评分统计

管理后台可以增加：

```text
评分分布：

5 星：80
4 星：30
3 星：10
2 星：5
1 星：3
```

这样管理员可以理解用户对菜品的反馈。

---

# 100. 图片处理

图片上传需要限制：

```text
图片类型
最大文件大小
文件扩展名
Content-Type
文件名
```

不要信任用户直接上传的文件名。

文件名应该由服务端生成。

---

# 101. Docker

必须提供：

```text
Dockerfile
docker-compose.yml
```

Compose 至少：

```text
postgres
redis
server
nginx
```

可以根据项目需要增加：

```text
minio
```

---

# 102. Docker 网络

内部服务统一通过 Docker Network 通信。

例如：

```text
server → postgres
server → redis
nginx → server
```

生产环境不需要把数据库和 Redis 端口直接暴露到公网。

---

# 103. PostgreSQL Docker

例如：

```env
POSTGRES_DB=clumsy_kitchen
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
```

实际配置必须允许通过 `.env` 注入。

---

# 104. Nginx

至少提供：

```text
API 反向代理
管理后台静态资源
HTTPS
gzip
上传大小限制
```

例如：

```text
https://example.com/api/
```

转发：

```text
http://server:8080
```

---

# 105. Swagger

提供：

```text
OpenAPI
Swagger UI
```

例如：

```text
http://localhost:8080/swagger-ui/index.html
```

实际地址以项目实现为准。

README 必须写明。

---

# 106. Actuator

可以使用 Spring Boot Actuator：

```text
/actuator/health
```

检查：

```text
应用
PostgreSQL
Redis
DataSource
```

生产环境只暴露必要 Endpoint。

---

# 107. 测试

至少测试：

```text
微信登录
管理员登录
RBAC
菜品创建
菜品上下架
菜品删除
库存扣减
库存并发
预约创建
预约取消
订单状态流转
评论资格
重复评论
评分计算
隐藏评论后的评分
```

---

# 108. 核心集成测试

测试：

```text
创建菜品
↓
设置库存
↓
上架
↓
用户登录
↓
预约
↓
库存减少
↓
管理员确认
↓
开始准备
↓
准备完成
↓
完成
↓
5 星评价
↓
rating = 5.00
```

继续：

```text
另一个用户 4 星
↓
rating = 4.50
```

继续：

```text
管理员隐藏 4 星评价
↓
rating 恢复为 5.00
```

---

# 109. Repository 测试

Repository 测试应重点覆盖：

```text
CRUD
分页
查询
事务
唯一约束
评论统计
库存更新
```

未来新增 MySQL 实现时，可以复用相同测试逻辑。

---

# 110. 前端构建检查

管理后台：

```bash
npm install
npm run build
```

小程序：

使用对应 Taro 构建命令生成微信小程序。

后端：

```bash
mvn clean test
```

并：

```bash
mvn package
```

确保没有编译错误。

---

# 111. README.md

必须生成完整 README。

至少：

```text
项目介绍
产品定位
核心功能
视觉风格
系统架构
技术栈
项目目录
环境要求
JDK
Node.js
PostgreSQL
Redis
微信开发者工具

数据库
Druid
多数据源
Flyway

本地开发
后端启动
小程序启动
管理后台启动

Swagger
环境变量
文件上传

Docker
Docker Compose
Nginx
HTTPS

生产部署
日志
监控
测试
常见问题
```

README 开头：

```text
🍳 clumsy_kitchen

笨拙一点没关系，
认真做饭就很好吃。
```

并明确：

> 当前版本完全不涉及金额、支付和商业交易。

---

# 112. README 数据库说明

必须写清楚：

```text
默认数据库：PostgreSQL
连接池：Druid
ORM / DAO：MyBatis-Plus
缓存：Redis
迁移：Flyway
```

并说明：

> PostgreSQL 是当前默认实现，不是业务层永久绑定的数据库。

---

# 113. README 多数据源说明

必须说明：

```text
当前默认：
PRIMARY → PostgreSQL → Druid

架构预留：
SECONDARY → 可配置数据库 → Druid
```

未来支持：

```text
MySQL
MariaDB
Oracle
SQL Server
```

时，主要修改：

```text
Infrastructure
DataSource
SQL / Dialect
```

而不是修改：

```text
Domain
Service
```

---

# 114. README 部署

必须提供完整：

```bash
git clone xxx

cd clumsy_kitchen

cp .env.example .env

docker compose build

docker compose up -d

docker compose ps

docker compose logs -f server
```

并说明：

```text
数据库初始化
环境变量
Nginx
HTTPS
域名
微信小程序合法域名
```

---

# 115. 代码质量

必须：

```text
命名清晰
职责单一
低耦合
高内聚
减少重复
避免巨型类
避免巨型组件
避免 any
避免硬编码
```

所有重要业务方法添加必要注释。

不要添加毫无意义的注释。

---

# 116. 前端组件设计

建立通用组件，例如：

```text
DishCard
StarRating
EmptyState
Loading
StatusBadge
OrderCard
RecipeStep
```

管理后台：

```text
SearchForm
PageTable
ConfirmDialog
ImageUploader
PermissionGuard
```

公共组件不要重复实现。

---

# 117. 前端状态管理

全局状态只保存：

```text
用户登录
权限
必要业务状态
```

页面临时状态使用 React：

```text
useState
useReducer
```

不要把全部页面状态塞进 Zustand。

---

# 118. 空状态

空订单：

```text
🍚

这里还空空的

去看看今天有什么好吃的吧
```

没有评论：

```text
🍳

还没有人分享味道

来做第一个试吃官吧！
```

加载：

```text
🍳 正在准备中...
```

---

# 119. 用户端文案规范

推荐：

```text
今天吃点什么呢？

厨房已经准备好啦

今天还有几份

厨房收到啦！

正在认真准备中...

做好啦，可以开吃啦！

谢谢你的反馈 ♡
```

避免：

```text
商品
消费
购买
支付
付款
金额
优惠
折扣
```

---

# 120. 订单文案规范

推荐：

```text
待确认
厨房马上看看

已确认
厨房收到啦

准备中
正在认真准备

已完成
做好啦，可以开吃啦

已取消
这次预约取消啦
```

可根据实际 UX 优化，但始终保持生活化。

---

# 121. 管理后台风格

管理后台也要保持：

```text
清爽
温暖
简洁
轻量
```

但不能牺牲效率。

后台核心仍然是：

```text
表格
筛选
分页
表单
批量操作
权限
统计
```

不要大量使用装饰性动画。

---

# 122. 动画原则

可以使用：

```text
淡入
轻微缩放
点击反馈
卡片进入
```

禁止大量：

```text
旋转
粒子
跳动
复杂动画
```

目标：

> 让界面有生命力，而不是干扰操作。

---

# 123. 数据库金额字段禁令

整个项目：

```text
数据库
Entity
DTO
VO
Java
TypeScript
API
React
Dashboard
README
```

都不得新增：

```text
price
amount
total_amount
sale_price
original_price
payment
payment_status
payment_method
transaction_id
currency
refund
revenue
profit
sales_amount
```

---

# 124. 商业化禁令

AI 在后续开发过程中，不得自行增加：

```text
购物车金额
支付页面
订单金额
微信支付
优惠券
商品价格
销售统计
```

除非用户未来明确提出新的商业化需求。

---

# 125. 开发阶段

不要一次性生成所有代码。

严格按照：

```text
Phase 1
项目初始化

Phase 2
数据库与迁移

Phase 3
后端基础架构

Phase 4
认证与 RBAC

Phase 5
分类与菜品

Phase 6
菜谱

Phase 7
预约单与库存

Phase 8
评论与评分

Phase 9
微信小程序

Phase 10
管理后台

Phase 11
Redis

Phase 12
文件存储

Phase 13
Docker / Nginx

Phase 14
Swagger / README

Phase 15
自动化测试

Phase 16
最终构建验证
```

---

# 126. 每一个阶段必须执行

每阶段完成：

```text
检查依赖
↓
检查代码
↓
编译
↓
测试
↓
修复问题
↓
检查类型
↓
进入下一阶段
```

不能把大量错误留到项目最后才处理。

---

# 127. 遇到技术冲突

如果：

```text
Spring Boot
Druid
MyBatis-Plus
PostgreSQL
多数据源
```

之间出现兼容性问题：

优先：

```text
稳定性
兼容性
可维护性
官方支持
```

而不是为了某个库的最新版强行组合。

如果某个库版本存在问题：

> 选择兼容的稳定版本，并在 README 记录原因。

---

# 128. 遇到数据库差异

如果 PostgreSQL 实现使用了：

```text
JSONB
UUID
特定函数
特定 SQL
```

必须判断：

> 该功能是否属于真正的业务要求。

如果只是方便开发，而会严重阻碍数据库替换，则优先考虑抽象或兼容设计。

如果确实需要数据库特性：

> 限制到 Infrastructure 层。

---

# 129. 未来多数据源

架构需要能够支持：

## 方案 A

```text
PRIMARY
PostgreSQL
```

## 方案 B

```text
PRIMARY
PostgreSQL

SECONDARY
MySQL
```

## 方案 C

```text
PRIMARY
PostgreSQL

REPORTING
PostgreSQL
```

## 方案 D

不同租户使用不同数据库。

但当前版本：

> 不强制实现完整多租户。

---

# 130. 跨数据库事务

当前版本：

> 不实现分布式事务。

如果未来需要：

```text
数据库 A
+
数据库 B
```

共同提交事务：

必须重新设计。

候选方案：

```text
Outbox
消息队列
最终一致性
分布式事务
```

不能误认为：

```java
@Transactional
```

天然解决跨数据库事务。

---

# 131. 最终验收

只有以下条件全部满足，项目才算完成：

```text
✅ 后端可以编译
✅ 后端可以启动
✅ PostgreSQL 可以初始化
✅ Druid 正常工作
✅ Redis 正常工作
✅ 微信登录可用
✅ 管理员登录可用
✅ JWT 可用
✅ RBAC 可用
✅ 分类可管理
✅ 菜品可管理
✅ 菜品可上下架
✅ 菜谱可管理
✅ 库存/份数可控制
✅ 用户可以提交预约
✅ 厨房可以处理预约
✅ 订单状态正确
✅ 用户可以评价
✅ 评价必须来自真实完成订单
✅ 评分限制 1~5 星
✅ 菜品平均评分自动计算
✅ 评论隐藏后评分自动变化
✅ 用户只能访问自己的数据
✅ 管理员权限正确
✅ 管理后台可构建
✅ 微信小程序可构建
✅ Docker 可以运行
✅ Nginx 配置完整
✅ Swagger 可访问
✅ README 完整
✅ 自动化测试通过
```

---

# 132. 最终交付物

最终必须生成：

```text
clumsy_kitchen/
├── server/
├── miniapp/
├── admin-web/
├── sql/
├── deploy/
├── docs/
├── .env.example
├── .gitignore
└── README.md
```

以及：

```text
Dockerfile
docker-compose.yml
Nginx 配置
PostgreSQL Migration
测试代码
OpenAPI
环境变量示例
```

---

# 133. 最终开发原则

整个项目开发过程中始终遵守以下原则：

```text
第一：
先保证业务正确。

第二：
再保证架构清晰。

第三：
再保证代码可维护。

第四：
再做性能优化。

第五：
再做视觉细节。

不要本末倒置。
```

核心目标：

> **构建一个真正可以运行、测试、部署、维护和继续扩展的 clumsy_kitchen，而不是生成一套演示代码。**

最终架构原则：

```text
可运行
可测试
可部署
可维护
可扩展
数据库可替换
多数据源可扩展
不绑定商业支付
业务逻辑清晰
```

# END

</user_query>