DO $$
BEGIN
    IF to_regclass('public.activities') IS NOT NULL THEN
        IF to_regclass('public.activity_favorites') IS NOT NULL THEN
            DELETE FROM activity_favorites
            WHERE activity_id IN (SELECT id FROM activities WHERE source_type = 'LIFELONG_LEARNING');
        END IF;

        IF to_regclass('public.activity_images') IS NOT NULL THEN
            DELETE FROM activity_images
            WHERE activity_id IN (SELECT id FROM activities WHERE source_type = 'LIFELONG_LEARNING');
        END IF;

        IF to_regclass('public.activity_reviews') IS NOT NULL THEN
            DELETE FROM activity_reviews
            WHERE activity_id IN (SELECT id FROM activities WHERE source_type = 'LIFELONG_LEARNING');
        END IF;

        IF to_regclass('public.activity_tags') IS NOT NULL THEN
            DELETE FROM activity_tags
            WHERE activity_id IN (SELECT id FROM activities WHERE source_type = 'LIFELONG_LEARNING');
        END IF;

        DELETE FROM activities
        WHERE source_type = 'LIFELONG_LEARNING';
    END IF;

    IF to_regclass('public.raw_activities') IS NOT NULL THEN
        DELETE FROM raw_activities
        WHERE source_type = 'LIFELONG_LEARNING';
    END IF;
END $$;
