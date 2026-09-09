-- 健康主体绑定组织与创建人，供 DataScope（ALL/SELF/ORG/ORG_CHILD）裁剪。

ALTER TABLE health_subject
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_health_subject_org_unit ON health_subject (org_unit_id);
CREATE INDEX IF NOT EXISTS idx_health_subject_created_by ON health_subject (created_by);

COMMENT ON COLUMN health_subject.org_unit_id IS '所属组织 ID（sys_org_unit），ORG/ORG_CHILD 范围过滤';
COMMENT ON COLUMN health_subject.created_by IS '创建人 sys_user.id，SELF 范围过滤';

-- 历史空值由 V23 回填至总部管理员；新建主体由应用写入当前管理员组织/创建人。
