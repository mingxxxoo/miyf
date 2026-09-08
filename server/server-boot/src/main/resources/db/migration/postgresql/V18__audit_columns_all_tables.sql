-- 统一审计列：所有业务表具备 create_time + last_modify_time（幂等）。

CREATE OR REPLACE FUNCTION miyf_add_timestamptz_if_missing(p_table TEXT, p_column TEXT)
    RETURNS VOID
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.tables
               WHERE table_schema = 'public'
                 AND table_name = p_table)
        AND NOT EXISTS (SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = p_table
                          AND column_name = p_column) THEN
        EXECUTE format(
                'ALTER TABLE %I ADD COLUMN %I TIMESTAMPTZ NOT NULL DEFAULT NOW()',
                p_table, p_column);
    END IF;
END;
$$;

-- 关联表：补 last_modify_time
SELECT miyf_add_timestamptz_if_missing('sys_user_role', 'last_modify_time');
SELECT miyf_add_timestamptz_if_missing('sys_perm_group_item', 'last_modify_time');
SELECT miyf_add_timestamptz_if_missing('sys_role_perm_group', 'last_modify_time');

-- 任务状态：补 create_time
SELECT miyf_add_timestamptz_if_missing('sys_job_state', 'create_time');

-- 兜底：公开 schema 下缺列的表统一补齐（跳过 flyway 元表）
DO
$$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN
            SELECT t.table_name
            FROM information_schema.tables t
            WHERE t.table_schema = 'public'
              AND t.table_type = 'BASE TABLE'
              AND t.table_name NOT LIKE 'flyway_%'
            LOOP
                PERFORM miyf_add_timestamptz_if_missing(r.table_name, 'create_time');
                PERFORM miyf_add_timestamptz_if_missing(r.table_name, 'last_modify_time');
            END LOOP;
    END;
$$;

DROP FUNCTION IF EXISTS miyf_add_timestamptz_if_missing(TEXT, TEXT);
