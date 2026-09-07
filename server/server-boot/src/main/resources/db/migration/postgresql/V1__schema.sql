-- miyf schema (PostgreSQL) — 雪花 BIGINT 主键；IAM + kitchen
-- 删除策略：物理删除（无 deleted 列）；子表按需 CASCADE

CREATE TABLE sys_org_unit (
    id              BIGINT       PRIMARY KEY,
    parent_id       BIGINT,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_org_unit_code UNIQUE (code)
);

CREATE TABLE sys_user (
    id              BIGINT       PRIMARY KEY,
    org_unit_id     BIGINT       REFERENCES sys_org_unit(id),
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nickname        VARCHAR(64),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE INDEX idx_sys_user_org ON sys_user(org_unit_id);

CREATE TABLE sys_role (
    id              BIGINT       PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    description     VARCHAR(255),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE sys_permission (
    id              BIGINT       PRIMARY KEY,
    code            VARCHAR(128) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(255),
    group_code      VARCHAR(64),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

CREATE TABLE sys_perm_group (
    id              BIGINT       PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(255),
    sort_order      INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_perm_group_code UNIQUE (code)
);

CREATE TABLE sys_perm_group_item (
    id              BIGINT       PRIMARY KEY,
    group_id        BIGINT       NOT NULL REFERENCES sys_perm_group(id) ON DELETE CASCADE,
    permission_id   BIGINT       NOT NULL REFERENCES sys_permission(id) ON DELETE CASCADE,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_perm_group_item UNIQUE (group_id, permission_id)
);

CREATE TABLE sys_role_perm_group (
    id              BIGINT       PRIMARY KEY,
    role_id         BIGINT       NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    group_id        BIGINT       NOT NULL REFERENCES sys_perm_group(id) ON DELETE CASCADE,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_role_perm_group UNIQUE (role_id, group_id)
);

CREATE TABLE sys_user_role (
    id              BIGINT       PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id         BIGINT       NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE sys_menu (
    id              BIGINT       PRIMARY KEY,
    parent_id       BIGINT,
    name            VARCHAR(64)  NOT NULL,
    path            VARCHAR(255),
    component       VARCHAR(255),
    icon            VARCHAR(64),
    menu_type       VARCHAR(16)  NOT NULL DEFAULT 'MENU',
    permission_code VARCHAR(128),
    sort_order      INT          NOT NULL DEFAULT 0,
    visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sys_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON'))
);

CREATE INDEX idx_sys_menu_parent ON sys_menu(parent_id);

-- ========== kitchen C 端用户 ==========
CREATE TABLE kitchen_user (
    id              BIGINT       PRIMARY KEY,
    openid          VARCHAR(64)  NOT NULL,
    unionid         VARCHAR(64),
    username        VARCHAR(64),
    nickname        VARCHAR(64),
    phone           VARCHAR(20),
    wechat_id       VARCHAR(64),
    avatar_url      VARCHAR(512),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitchen_user_openid UNIQUE (openid)
);

CREATE TABLE dish_category (
    id              BIGINT       PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL,
    icon            VARCHAR(512),
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE dish (
    id              BIGINT       PRIMARY KEY,
    category_id     BIGINT       REFERENCES dish_category(id),
    name            VARCHAR(128) NOT NULL,
    subtitle        VARCHAR(255),
    description     TEXT,
    cover_image     VARCHAR(512),
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    sort_order      INT          NOT NULL DEFAULT 0,
    is_recommend    BOOLEAN      NOT NULL DEFAULT FALSE,
    stock           INT          NOT NULL DEFAULT 0,
    stock_type      VARCHAR(32)  NOT NULL DEFAULT 'LIMITED',
    unit            VARCHAR(32)  NOT NULL DEFAULT '份',
    rating          NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count    INT          NOT NULL DEFAULT 0,
    created_by      BIGINT,
    updated_by      BIGINT,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_dish_stock_type CHECK (stock_type IN ('LIMITED', 'UNLIMITED')),
    CONSTRAINT ck_dish_status CHECK (status IN ('DRAFT', 'ON_SALE', 'OFF_SALE')),
    CONSTRAINT ck_dish_stock_nonneg CHECK (stock >= 0)
);

CREATE INDEX idx_dish_category_id ON dish(category_id);
CREATE INDEX idx_dish_status ON dish(status);
CREATE INDEX idx_dish_sort ON dish(sort_order);
CREATE INDEX idx_dish_recommend ON dish(is_recommend);

CREATE TABLE dish_image (
    id              BIGINT       PRIMARY KEY,
    dish_id         BIGINT       NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    image_url       VARCHAR(512) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dish_image_dish_id ON dish_image(dish_id);

CREATE TABLE dish_recipe (
    id              BIGINT       PRIMARY KEY,
    dish_id         BIGINT       NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    description     TEXT,
    difficulty      VARCHAR(32),
    prepare_minutes INT,
    cook_minutes    INT,
    servings        INT,
    ingredients     JSONB        NOT NULL DEFAULT '[]'::jsonb,
    seasonings      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    steps           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    tips            TEXT,
    nutrition       JSONB,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_dish_recipe_dish_id UNIQUE (dish_id)
);

CREATE TABLE kitchen_order (
    id              BIGINT       PRIMARY KEY,
    order_no        VARCHAR(32)  NOT NULL,
    user_id         BIGINT       NOT NULL REFERENCES kitchen_user(id),
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    remark          VARCHAR(512),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitchen_order_no UNIQUE (order_no),
    CONSTRAINT ck_kitchen_order_status CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_kitchen_order_user_id ON kitchen_order(user_id);
CREATE INDEX idx_kitchen_order_status ON kitchen_order(status);
CREATE INDEX idx_kitchen_order_create_time ON kitchen_order(create_time);

CREATE TABLE kitchen_order_item (
    id              BIGINT       PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES kitchen_order(id) ON DELETE CASCADE,
    dish_id         BIGINT       NOT NULL REFERENCES dish(id),
    dish_name       VARCHAR(128) NOT NULL,
    quantity        INT          NOT NULL,
    unit            VARCHAR(32)  NOT NULL DEFAULT '份',
    remark          VARCHAR(255),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_kitchen_order_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_kitchen_order_item_order_id ON kitchen_order_item(order_id);
CREATE INDEX idx_kitchen_order_item_dish_id ON kitchen_order_item(dish_id);

CREATE TABLE kitchen_comment (
    id              BIGINT       PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES kitchen_user(id),
    dish_id         BIGINT       NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    order_id        BIGINT       NOT NULL REFERENCES kitchen_order(id),
    rating          INT          NOT NULL,
    content         VARCHAR(1000),
    status          VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitchen_comment_order_dish_user UNIQUE (order_id, dish_id, user_id),
    CONSTRAINT ck_kitchen_comment_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT ck_kitchen_comment_status CHECK (status IN ('NORMAL', 'HIDDEN'))
);

CREATE INDEX idx_kitchen_comment_dish_id ON kitchen_comment(dish_id);
CREATE INDEX idx_kitchen_comment_user_id ON kitchen_comment(user_id);

CREATE TABLE kitchen_comment_image (
    id              BIGINT       PRIMARY KEY,
    comment_id      BIGINT       NOT NULL REFERENCES kitchen_comment(id) ON DELETE CASCADE,
    image_url       VARCHAR(512) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kitchen_comment_image_comment_id ON kitchen_comment_image(comment_id);

CREATE TABLE operation_logs (
    id                BIGINT       PRIMARY KEY,
    operator_id       BIGINT,
    operator_name     VARCHAR(64),
    operation_type    VARCHAR(64)  NOT NULL,
    target_type       VARCHAR(64),
    target_id         VARCHAR(64),
    request_ip        VARCHAR(64),
    request_method    VARCHAR(16),
    request_uri       VARCHAR(255),
    operation_detail  TEXT,
    create_time        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_operation_logs_create_time ON operation_logs(create_time);
CREATE INDEX idx_operation_logs_operator_id ON operation_logs(operator_id);
