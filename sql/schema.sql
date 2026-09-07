-- miyf schema (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    openid          VARCHAR(64)  NOT NULL,
    unionid         VARCHAR(64),
    nickname        VARCHAR(64),
    avatar_url      VARCHAR(512),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_users_openid UNIQUE (openid)
);

CREATE TABLE admin_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nickname        VARCHAR(64),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_admin_users_username UNIQUE (username)
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    description     VARCHAR(255),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    description     VARCHAR(255),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE admin_user_roles (
    admin_user_id   UUID NOT NULL REFERENCES admin_users(id),
    role_id         UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (admin_user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id),
    permission_id   UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE dish_category (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(64)  NOT NULL,
    icon            VARCHAR(512),
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE dish (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id     UUID         REFERENCES dish_category(id),
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
    created_by      UUID,
    updated_by      UUID,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_dish_stock_type CHECK (stock_type IN ('LIMITED', 'UNLIMITED')),
    CONSTRAINT ck_dish_status CHECK (status IN ('DRAFT', 'ON_SALE', 'OFF_SALE')),
    CONSTRAINT ck_dish_stock_nonneg CHECK (stock >= 0)
);

CREATE INDEX idx_dish_category_id ON dish(category_id);
CREATE INDEX idx_dish_status ON dish(status);
CREATE INDEX idx_dish_sort ON dish(sort_order);
CREATE INDEX idx_dish_recommend ON dish(is_recommend);

CREATE TABLE dish_image (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id         UUID         NOT NULL REFERENCES dish(id),
    image_url       VARCHAR(512) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dish_image_dish_id ON dish_image(dish_id);

CREATE TABLE dish_recipe (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id         UUID         NOT NULL REFERENCES dish(id),
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
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_dish_recipe_dish_id_active
    ON dish_recipe (dish_id)
    WHERE deleted = FALSE;

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_no        VARCHAR(32)  NOT NULL,
    user_id         UUID         NOT NULL REFERENCES users(id),
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    remark          VARCHAR(512),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT ck_orders_status CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_create_time ON orders(create_time);

CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID         NOT NULL REFERENCES orders(id),
    dish_id         UUID         NOT NULL REFERENCES dish(id),
    dish_name       VARCHAR(128) NOT NULL,
    quantity        INT          NOT NULL,
    unit            VARCHAR(32)  NOT NULL DEFAULT '份',
    remark          VARCHAR(255),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_dish_id ON order_items(dish_id);

CREATE TABLE comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id),
    dish_id         UUID         NOT NULL REFERENCES dish(id),
    order_id        UUID         NOT NULL REFERENCES orders(id),
    rating          INT          NOT NULL,
    content         VARCHAR(1000),
    status          VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_comments_order_dish_user UNIQUE (order_id, dish_id, user_id),
    CONSTRAINT ck_comments_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT ck_comments_status CHECK (status IN ('NORMAL', 'HIDDEN'))
);

CREATE INDEX idx_comments_dish_id ON comments(dish_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_order_id ON comments(order_id);
CREATE INDEX idx_comments_status ON comments(status);

CREATE TABLE comment_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id      UUID         NOT NULL REFERENCES comments(id),
    image_url       VARCHAR(512) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_images_comment_id ON comment_images(comment_id);

CREATE TABLE operation_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operator_id       UUID,
    operator_name     VARCHAR(64),
    operation_type    VARCHAR(64)  NOT NULL,
    target_type       VARCHAR(64),
    target_id         VARCHAR(64),
    request_ip        VARCHAR(64),
    request_method    VARCHAR(16),
    request_uri       VARCHAR(255),
    operation_detail  TEXT,
    create_time        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_operation_logs_create_time ON operation_logs(create_time);
CREATE INDEX idx_operation_logs_operator_id ON operation_logs(operator_id);
