-- 兼容已落地库：将 *_at 审计/业务时间列统一为 *_time。
-- 全新安装若历史脚本已是新列名，本脚本会跳过（幂等）。

CREATE OR REPLACE FUNCTION miyf_rename_column_if_exists(p_table TEXT, p_old TEXT, p_new TEXT)
    RETURNS VOID
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name = p_table
                 AND column_name = p_old)
        AND NOT EXISTS (SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = p_table
                          AND column_name = p_new) THEN
        EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', p_table, p_old, p_new);
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION miyf_rename_index_if_exists(p_old TEXT, p_new TEXT)
    RETURNS VOID
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
               WHERE n.nspname = 'public' AND c.relkind = 'i' AND c.relname = p_old)
        AND NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public' AND c.relkind = 'i' AND c.relname = p_new) THEN
        EXECUTE format('ALTER INDEX %I RENAME TO %I', p_old, p_new);
    END IF;
END;
$$;

DO
$$
    DECLARE
        t TEXT;
        both_cols TEXT[] := ARRAY[
            'sys_org_unit', 'sys_user', 'sys_role', 'sys_permission', 'sys_perm_group', 'sys_menu',
            'kitchen_user', 'dish_category', 'dish', 'dish_image', 'dish_recipe',
            'kitchen_order', 'kitchen_order_item', 'kitchen_comment', 'kitchen_comment_image',
            'operation_logs',
            'sys_config', 'sys_dict_type', 'sys_dict_item',
            'health_subject', 'health_sample', 'health_provider_binding', 'health_sync_run',
            'sys_inbox_message', 'sys_app',
            'sys_notify_template', 'sys_notify_send_log'
            ];
        create_only TEXT[] := ARRAY[
            'sys_perm_group_item', 'sys_role_perm_group', 'sys_user_role'
            ];
    BEGIN
        FOREACH t IN ARRAY both_cols
            LOOP
                PERFORM miyf_rename_column_if_exists(t, 'created_at', 'create_time');
                PERFORM miyf_rename_column_if_exists(t, 'updated_at', 'last_modify_time');
            END LOOP;

        FOREACH t IN ARRAY create_only
            LOOP
                PERFORM miyf_rename_column_if_exists(t, 'created_at', 'create_time');
            END LOOP;

        -- job state 仅有更新时间
        PERFORM miyf_rename_column_if_exists('sys_job_state', 'updated_at', 'last_modify_time');

        -- 业务时间列
        PERFORM miyf_rename_column_if_exists('health_sample', 'measured_at', 'measured_time');
        PERFORM miyf_rename_column_if_exists('health_provider_binding', 'last_sync_at', 'last_sync_time');
        PERFORM miyf_rename_column_if_exists('health_sync_run', 'started_at', 'started_time');
        PERFORM miyf_rename_column_if_exists('health_sync_run', 'finished_at', 'finished_time');
    END;
$$;

SELECT miyf_rename_index_if_exists('idx_kitchen_order_created_at', 'idx_kitchen_order_create_time');
SELECT miyf_rename_index_if_exists('idx_operation_logs_created_at', 'idx_operation_logs_create_time');

DROP FUNCTION IF EXISTS miyf_rename_column_if_exists(TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS miyf_rename_index_if_exists(TEXT, TEXT);
