DO $$
BEGIN
    IF to_regclass('public.activities') IS NOT NULL THEN
        ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_source_type_check;
        ALTER TABLE activities ADD CONSTRAINT activities_source_type_check
            CHECK (source_type IN (
                'KOPIS',
                'EXHIBITION',
                'SEOUL_CULTURE',
                'SEOUL_RESERVATION',
                'DISCOVERY',
                'URL_MANUAL'
            ));
    END IF;

    IF to_regclass('public.raw_activities') IS NOT NULL THEN
        ALTER TABLE raw_activities DROP CONSTRAINT IF EXISTS raw_activities_source_type_check;
        ALTER TABLE raw_activities ADD CONSTRAINT raw_activities_source_type_check
            CHECK (source_type IN (
                'KOPIS',
                'EXHIBITION',
                'SEOUL_CULTURE',
                'SEOUL_RESERVATION',
                'DISCOVERY',
                'URL_MANUAL'
            ));
    END IF;
END $$;
