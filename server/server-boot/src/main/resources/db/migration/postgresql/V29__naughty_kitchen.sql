-- 胡闹厨房：厨房实体、绑定、邀请、角色字段、菜品审核与订单厨房维度（无存量回填）

-- ========== kitchen ==========
CREATE TABLE kitchen (
    id               BIGINT       PRIMARY KEY,
    owner_user_id    BIGINT       NOT NULL REFERENCES kitchen_user(id),
    name             VARCHAR(128) NOT NULL,
    intro            VARCHAR(1000),
    cover_image      VARCHAR(512),
    status           VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitchen_owner UNIQUE (owner_user_id),
    CONSTRAINT ck_kitchen_status CHECK (status IN ('OPEN', 'CLOSED', 'BANNED'))
);

CREATE INDEX idx_kitchen_status ON kitchen(status);

-- ========== kitchen_binding ==========
CREATE TABLE kitchen_binding (
    id               BIGINT       PRIMARY KEY,
    kitchen_id       BIGINT       NOT NULL REFERENCES kitchen(id),
    diner_user_id    BIGINT       NOT NULL REFERENCES kitchen_user(id),
    status           VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    reject_reason    VARCHAR(512),
    applied_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_at       TIMESTAMPTZ,
    unbound_at       TIMESTAMPTZ,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_kitchen_binding_status CHECK (status IN ('PENDING', 'BOUND', 'REJECTED', 'UNBOUND'))
);

CREATE INDEX idx_kitchen_binding_kitchen ON kitchen_binding(kitchen_id, status);
CREATE INDEX idx_kitchen_binding_diner ON kitchen_binding(diner_user_id, status);
-- 一客一厨：同时仅允许一条已绑定
CREATE UNIQUE INDEX uk_kitchen_binding_diner_bound
    ON kitchen_binding(diner_user_id)
    WHERE status = 'BOUND';
-- 同厨同时仅一条待确认
CREATE UNIQUE INDEX uk_kitchen_binding_pending
    ON kitchen_binding(kitchen_id, diner_user_id)
    WHERE status = 'PENDING';

-- ========== kitchen_invite ==========
CREATE TABLE kitchen_invite (
    id               BIGINT       PRIMARY KEY,
    kitchen_id       BIGINT       NOT NULL REFERENCES kitchen(id),
    code             VARCHAR(32)  NOT NULL,
    token            VARCHAR(64)  NOT NULL,
    expire_at        TIMESTAMPTZ,
    max_uses         INT,
    used_count       INT          NOT NULL DEFAULT 0,
    status           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kitchen_invite_code UNIQUE (code),
    CONSTRAINT uk_kitchen_invite_token UNIQUE (token),
    CONSTRAINT ck_kitchen_invite_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_kitchen_invite_used_nonneg CHECK (used_count >= 0)
);

CREATE INDEX idx_kitchen_invite_kitchen ON kitchen_invite(kitchen_id, status);

-- ========== kitchen_user 角色 ==========
ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS active_role VARCHAR(32),
    ADD COLUMN IF NOT EXISTS is_chef BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_diner BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS role_chosen_at TIMESTAMPTZ;

ALTER TABLE kitchen_user
    DROP CONSTRAINT IF EXISTS ck_kitchen_user_active_role;
ALTER TABLE kitchen_user
    ADD CONSTRAINT ck_kitchen_user_active_role
        CHECK (active_role IS NULL OR active_role IN ('CHEF', 'DINER'));

-- ========== dish 厨房维度与审核 ==========
ALTER TABLE dish
    ADD COLUMN IF NOT EXISTS kitchen_id BIGINT REFERENCES kitchen(id),
    ADD COLUMN IF NOT EXISTS audit_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS process_instance_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(1000);

ALTER TABLE dish
    DROP CONSTRAINT IF EXISTS ck_dish_audit_status;
ALTER TABLE dish
    ADD CONSTRAINT ck_dish_audit_status
        CHECK (audit_status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'));

CREATE INDEX IF NOT EXISTS idx_dish_kitchen_id ON dish(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_dish_audit_status ON dish(audit_status);

-- ========== kitchen_order ==========
ALTER TABLE kitchen_order
    ADD COLUMN IF NOT EXISTS kitchen_id BIGINT REFERENCES kitchen(id);

CREATE INDEX IF NOT EXISTS idx_kitchen_order_kitchen_id ON kitchen_order(kitchen_id);

-- ========== 管理菜单：胡闹厨房 + 审核/厨房/绑定 ==========
UPDATE sys_menu SET name = '胡闹厨房' WHERE id = 50100;
UPDATE sys_app SET name = '胡闹厨房', description = '厨师与食客互动的厨房平台' WHERE code = 'kitchen';

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible, product)
VALUES
 (50108, 50100, '菜品审核', '/kitchen/audits', 'kitchen/DishAudits', 'AuditOutlined', 'MENU', 'kitchen:dish:audit', 0, TRUE, 'kitchen'),
 (50109, 50100, '厨房', '/kitchen/kitchens', 'kitchen/Kitchens', 'HomeOutlined', 'MENU', 'kitchen:kitchen:list', 8, TRUE, 'kitchen'),
 (50110, 50100, '绑定关系', '/kitchen/bindings', 'kitchen/Bindings', 'LinkOutlined', 'MENU', 'kitchen:binding:list', 9, TRUE, 'kitchen')
ON CONFLICT (id) DO NOTHING;
