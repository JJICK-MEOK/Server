DO $$
BEGIN
    IF to_regclass('public.activities') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'activities'
              AND column_name = 'activity_start_at'
        ) THEN
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'activities'
                  AND column_name = 'start_at'
            ) THEN
                EXECUTE 'UPDATE activities
                         SET start_at = COALESCE(start_at, activity_start_at)
                         WHERE activity_start_at IS NOT NULL';
            ELSE
                EXECUTE 'ALTER TABLE activities RENAME COLUMN activity_start_at TO start_at';
            END IF;

            EXECUTE 'ALTER TABLE activities DROP COLUMN IF EXISTS activity_start_at';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'activities'
              AND column_name = 'activity_end_at'
        ) THEN
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'activities'
                  AND column_name = 'end_at'
            ) THEN
                EXECUTE 'UPDATE activities
                         SET end_at = COALESCE(end_at, activity_end_at)
                         WHERE activity_end_at IS NOT NULL';
            ELSE
                EXECUTE 'ALTER TABLE activities RENAME COLUMN activity_end_at TO end_at';
            END IF;

            EXECUTE 'ALTER TABLE activities DROP COLUMN IF EXISTS activity_end_at';
        END IF;

        EXECUTE 'ALTER TABLE activities DROP COLUMN IF EXISTS age_range';
        EXECUTE 'ALTER TABLE activities DROP COLUMN IF EXISTS location';
        EXECUTE 'ALTER TABLE activities DROP COLUMN IF EXISTS uri';
    END IF;
END $$;
