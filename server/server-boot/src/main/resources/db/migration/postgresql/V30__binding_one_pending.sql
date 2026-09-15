-- 一客同时仅一条待确认绑定（与 BOUND 唯一约束对称）
CREATE UNIQUE INDEX IF NOT EXISTS uk_kitchen_binding_diner_pending
    ON kitchen_binding(diner_user_id)
    WHERE status = 'PENDING';
