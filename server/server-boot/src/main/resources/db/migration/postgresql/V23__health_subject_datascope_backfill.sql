-- 回填历史健康主体的组织与创建人，避免 SELF/ORG 范围下永久不可见。
-- 通过组织编码 HQ、用户名 admin 动态解析 ID，避免硬编码种子主键。

DO
$$
    DECLARE
        v_org_id   BIGINT;
        v_admin_id BIGINT;
    BEGIN
        SELECT id
        INTO v_org_id
        FROM sys_org_unit
        WHERE code = 'HQ'
        ORDER BY id
        LIMIT 1;

        SELECT id
        INTO v_admin_id
        FROM sys_user
        WHERE lower(username) = 'admin'
        ORDER BY id
        LIMIT 1;

        IF v_org_id IS NULL OR v_admin_id IS NULL THEN
            RAISE NOTICE 'skip health_subject datascope backfill: HQ org or admin user not found';
            RETURN;
        END IF;

        UPDATE health_subject
        SET remark      = CASE
                              WHEN remark IS NULL OR remark = '' THEN '[datascope-backfill]'
                              WHEN remark NOT LIKE '%[datascope-backfill]%'
                                  THEN remark || ' [datascope-backfill]'
                              ELSE remark
            END,
            org_unit_id = COALESCE(org_unit_id, v_org_id),
            created_by  = COALESCE(created_by, v_admin_id)
        WHERE org_unit_id IS NULL
           OR created_by IS NULL;
    END
$$;
