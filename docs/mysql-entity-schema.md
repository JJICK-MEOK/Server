# MySQL Entity Schema

기준: `src/main/resources/db/migration/mysql/V1__init_mysql_schema.sql` + JPA entity 매핑.

공통 규칙:
- 엔진/문자셋: 모든 테이블 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`.
- 기본 PK: `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`.
- 일반 엔티티 공통 컬럼: `created_at DATETIME(6) NULL`, `updated_at DATETIME(6) NULL` (`BaseEntity`).
- 개인화 벡터 엔티티 공통 컬럼: `created_at DATETIME(6) NOT NULL`, `updated_at DATETIME(6) NOT NULL` (`BaseTimeEntity`).
- enum 매핑: JPA `EnumType.STRING`, DB는 대부분 `VARCHAR`. DB `CHECK`는 `source_type` 일부만 강제.
- 벡터 매핑: pgvector 없음. `embedding JSON NOT NULL`, Java `float[]`, `FloatArrayJsonConverter`가 14차원/null/NaN/Infinity 검증.
- 개인화 활동 추천 API는 위 저장 벡터를 사용하지 않는다. `PREFERENCE_TAG`를 `tags.id ASC`로 정렬해 요청 시 사용자/활동 `int[]` 이진 벡터를 만들고 Java에서 코사인 유사도를 계산한다. 기존 벡터 테이블은 레거시 호환용이다.

## Entity/Table Map

| Entity | Table |
|---|---|
| `Region` | `regions` |
| `User` | `users` |
| `UserProfile` | `user_profiles` |
| `UserOnboarding` | `user_onboardings` |
| `UserOnboardingRegion` | `user_onboarding_regions` |
| `UserOnboardingTag` | `user_onboarding_tags` |
| `Tag` | `tags` |
| `Activity` | `activities` |
| `ActivityTag` | `activity_tags` |
| `Image` | `activity_images` |
| `Favorite` | `activity_favorites` |
| `Review` | `activity_reviews` |
| `RawActivity` | `raw_activities` |
| `Advertisement` | `advertisements` |
| `ActivityPreferenceVector` | `activity_preference_vectors` |
| `UserPreferenceVector` | `user_preference_vectors` |
| `TagPreferenceVector` | `tag_preference_vectors` |

## Tables

### `regions`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `parent_id BIGINT NULL`
- `name VARCHAR(50) NOT NULL`
- `depth VARCHAR(255) NOT NULL`

제약:
- PK: `id`
- FK: `fk_regions_parent`: `parent_id` -> `regions(id)` `ON DELETE SET NULL`

앱 enum:
- `depth`: `RegionDepth` = `PROVINCE`, `DISTRICT`

관계:
- self parent-child region.

### `users`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `email VARCHAR(150) NULL`
- `password_hash VARCHAR(200) NULL`
- `auth_provider VARCHAR(20) NOT NULL`
- `provider_id VARCHAR(50) NULL`
- `role VARCHAR(20) NOT NULL`
- `registration_status VARCHAR(30) NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_users_email` (`email`)
- UNIQUE: `uk_users_provider` (`auth_provider`, `provider_id`)

앱 enum:
- `auth_provider`: `LOCAL`, `KAKAO`, `GOOGLE`, `NAVER`
- `role`: `USER`, `ADMIN`
- `registration_status`: `NOT_STARTED`, `PROFILE_COMPLETED`, `ONBOARDING_COMPLETED`

관계:
- 1:1 `user_profiles`
- 1:1 `user_onboardings`
- 1:1 `user_preference_vectors`
- 1:N `activity_favorites`
- 1:N `activity_reviews`

### `tags`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `name VARCHAR(50) NOT NULL`
- `type VARCHAR(50) NOT NULL`
- `tag_group_type VARCHAR(50) NULL`

제약:
- PK: `id`
- UNIQUE: `uk_tags_name_type` (`name`, `type`)

앱 enum:
- `type`: `TOPIC_CATEGORY`, `PREFERENCE_TAG`, `ACTIVITY_CATEGORY`
- `tag_group_type`: `MOOD`, `INTENSITY`, `PURPOSE`, `DURATION`, `SIZE`

관계:
- 1:N `activity_tags`
- 1:N `user_onboarding_tags`
- 1:1 `tag_preference_vectors`

### `advertisements`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `title VARCHAR(100) NOT NULL`
- `image_url VARCHAR(500) NOT NULL`
- `redirect_url VARCHAR(500) NOT NULL`
- `position VARCHAR(50) NOT NULL`
- `sort_order INT NOT NULL`
- `start_at DATETIME(6) NULL`
- `end_at DATETIME(6) NULL`
- `view_count INT NOT NULL`
- `click_count INT NOT NULL`
- `is_active BOOLEAN NOT NULL`

제약:
- PK: `id`

앱 enum:
- `position`: `ACTIVITY_LIST`, `MAIN_BANNER`

### `raw_activities`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `source_type VARCHAR(50) NOT NULL`
- `external_id VARCHAR(100) NULL`
- `request_url VARCHAR(1000) NOT NULL`
- `content_type VARCHAR(50) NOT NULL`
- `payload TEXT NOT NULL`

제약:
- PK: `id`
- CHECK: `raw_activities_source_type_check`: `source_type IN ('KOPIS','EXHIBITION','SEOUL_CULTURE','SEOUL_RESERVATION','DISCOVERY','URL_MANUAL')`

앱 enum:
- `source_type`: `SourceType`

### `user_profiles`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_id BIGINT NOT NULL`
- `nickname VARCHAR(10) NOT NULL`
- `birth_date DATE NOT NULL`
- `gender VARCHAR(20) NOT NULL`
- `status VARCHAR(20) NOT NULL`
- `profile_image_url VARCHAR(500) NULL`
- `service_terms_agreed BOOLEAN NOT NULL`
- `privacy_policy_agreed BOOLEAN NOT NULL`
- `marketing_agreed BOOLEAN NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_user_profiles_user_id` (`user_id`)
- UNIQUE: `uk_user_profiles_nickname` (`nickname`)
- FK: `fk_user_profiles_user`: `user_id` -> `users(id)` `ON DELETE CASCADE`

앱 enum:
- `gender`: `MALE`, `FEMALE`, `NONE`
- `status`: `STUDENT`, `WORKER`, `JOB_SEEKER`, `FREELANCER`, `ETC`

관계:
- one-to-one `users`.

### `user_onboardings`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_id BIGINT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_user_onboardings_user_id` (`user_id`)
- FK: `fk_user_onboardings_user`: `user_id` -> `users(id)` `ON DELETE CASCADE`

관계:
- one-to-one `users`
- one-to-many `user_onboarding_regions`
- one-to-many `user_onboarding_tags`

### `user_onboarding_regions`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_onboarding_id BIGINT NOT NULL`
- `region_id BIGINT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_user_onboarding_regions_onboarding_region` (`user_onboarding_id`, `region_id`)
- FK: `fk_user_onboarding_regions_onboarding`: `user_onboarding_id` -> `user_onboardings(id)` `ON DELETE CASCADE`
- FK: `fk_user_onboarding_regions_region`: `region_id` -> `regions(id)` `ON DELETE CASCADE`

역할:
- onboarding-region join entity.

### `user_onboarding_tags`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_onboarding_id BIGINT NOT NULL`
- `tag_id BIGINT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_user_onboarding_tags_onboarding_tag` (`user_onboarding_id`, `tag_id`)
- FK: `fk_user_onboarding_tags_onboarding`: `user_onboarding_id` -> `user_onboardings(id)` `ON DELETE CASCADE`
- FK: `fk_user_onboarding_tags_tag`: `tag_id` -> `tags(id)` `ON DELETE CASCADE`

역할:
- onboarding-tag join entity.

### `activities`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `region_id BIGINT NOT NULL`
- `title TEXT NOT NULL`
- `description TEXT NOT NULL`
- `thumbnail_url TEXT NULL`
- `source_url TEXT NOT NULL`
- `address TEXT NULL`
- `organizer TEXT NULL`
- `contact_info TEXT NULL`
- `target TEXT NULL`
- `start_at DATETIME(6) NULL`
- `end_at DATETIME(6) NULL`
- `recruit_start_at DATETIME(6) NULL`
- `recruit_end_at DATETIME(6) NULL`
- `price INT NOT NULL`
- `activity_type VARCHAR(50) NOT NULL`
- `category VARCHAR(50) NOT NULL`
- `source_type VARCHAR(50) NOT NULL`
- `external_id TEXT NULL`
- `approval_status VARCHAR(50) NOT NULL`
- `view_count INT NOT NULL`
- `like_count INT NOT NULL`
- `review_count INT NOT NULL`
- `is_active BOOLEAN NOT NULL`

제약:
- PK: `id`
- FK: `fk_activities_region`: `region_id` -> `regions(id)`
- CHECK: `activities_source_type_check`: `source_type IN ('KOPIS','EXHIBITION','SEOUL_CULTURE','SEOUL_RESERVATION','DISCOVERY','URL_MANUAL')`

앱 enum:
- `activity_type`: `PROGRAM`, `ONE_DAY`, `EVENT`, `CLUB`
- `category`: `SPORTS`, `CULTURE`, `CRAFT`, `COOKING`, `PHOTO_VIDEO`, `HUMANITIES`, `TRAVEL`, `LANGUAGE`, `VOLUNTEER`, `CAREER`
- `source_type`: `KOPIS`, `EXHIBITION`, `SEOUL_CULTURE`, `SEOUL_RESERVATION`, `DISCOVERY`, `URL_MANUAL`
- `approval_status`: `PENDING`, `APPROVED`, `REJECTED`

관계:
- many-to-one `regions`
- one-to-many `activity_tags` with JPA cascade/orphanRemoval
- one-to-many `activity_images`
- one-to-many `activity_favorites`
- one-to-many `activity_reviews`
- one-to-one `activity_preference_vectors`

### `activity_tags`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `activity_id BIGINT NOT NULL`
- `tag_id BIGINT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_activity_tags_activity_tag` (`activity_id`, `tag_id`)
- FK: `fk_activity_tags_activity`: `activity_id` -> `activities(id)` `ON DELETE CASCADE`
- FK: `fk_activity_tags_tag`: `tag_id` -> `tags(id)` `ON DELETE CASCADE`

역할:
- activity-tag join entity.

### `activity_images`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `activity_id BIGINT NOT NULL`
- `image_url VARCHAR(500) NOT NULL`
- `sort_order INT NOT NULL`
- `is_thumbnail BOOLEAN NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_activity_images_activity_sort_order` (`activity_id`, `sort_order`)
- FK: `fk_activity_images_activity`: `activity_id` -> `activities(id)` `ON DELETE CASCADE`

관계:
- many-to-one `activities`.

### `activity_favorites`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_id BIGINT NOT NULL`
- `activity_id BIGINT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_activity_favorites_user_activity` (`user_id`, `activity_id`)
- FK: `fk_activity_favorites_user`: `user_id` -> `users(id)` `ON DELETE CASCADE`
- FK: `fk_activity_favorites_activity`: `activity_id` -> `activities(id)` `ON DELETE CASCADE`

역할:
- user-activity favorite join entity.

### `activity_reviews`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `created_at DATETIME(6) NULL`
- `updated_at DATETIME(6) NULL`
- `user_id BIGINT NOT NULL`
- `activity_id BIGINT NOT NULL`
- `rating INT NOT NULL`
- `reason TEXT NULL`
- `like_count INT NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_activity_reviews_user_activity` (`user_id`, `activity_id`)
- FK: `fk_activity_reviews_user`: `user_id` -> `users(id)` `ON DELETE CASCADE`
- FK: `fk_activity_reviews_activity`: `activity_id` -> `activities(id)` `ON DELETE CASCADE`

관계:
- many-to-one `users`
- many-to-one `activities`

### `activity_preference_vectors`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `activity_id BIGINT NOT NULL`
- `embedding JSON NOT NULL`
- `vector_version INT NOT NULL`
- `created_at DATETIME(6) NOT NULL`
- `updated_at DATETIME(6) NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_activity_preference_vectors_activity_id` (`activity_id`)
- FK: `fk_activity_preference_vectors_activity`: `activity_id` -> `activities(id)` `ON DELETE CASCADE`

앱 제약:
- `embedding`: JSON array, 정확히 14개 `float`, null 금지, NaN/Infinity 금지.

관계:
- one-to-one `activities`.

### `user_preference_vectors`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `user_id BIGINT NOT NULL`
- `embedding JSON NOT NULL`
- `vector_version INT NOT NULL`
- `created_at DATETIME(6) NOT NULL`
- `updated_at DATETIME(6) NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_user_preference_vectors_user_id` (`user_id`)
- FK: `fk_user_preference_vectors_user`: `user_id` -> `users(id)` `ON DELETE CASCADE`

앱 제약:
- `embedding`: JSON array, 정확히 14개 `float`, null 금지, NaN/Infinity 금지.

관계:
- one-to-one `users`.

### `tag_preference_vectors`

컬럼:
- `id BIGINT NOT NULL AUTO_INCREMENT`
- `tag_id BIGINT NOT NULL`
- `embedding JSON NOT NULL`
- `vector_version INT NOT NULL`
- `created_at DATETIME(6) NOT NULL`
- `updated_at DATETIME(6) NOT NULL`

제약:
- PK: `id`
- UNIQUE: `uk_tag_preference_vectors_tag_id` (`tag_id`)
- FK: `fk_tag_preference_vectors_tag`: `tag_id` -> `tags(id)` `ON DELETE CASCADE`

앱 제약:
- `embedding`: JSON array, 정확히 14개 `float`, null 금지, NaN/Infinity 금지.

관계:
- one-to-one `tags`.

## Relationship Summary

- `regions.parent_id` -> `regions.id`: region tree.
- `activities.region_id` -> `regions.id`: activity belongs to region.
- `user_profiles.user_id` -> `users.id`: user profile, one-to-one.
- `user_onboardings.user_id` -> `users.id`: onboarding root, one-to-one.
- `user_onboarding_regions`: onboarding-region N:M join.
- `user_onboarding_tags`: onboarding-tag N:M join.
- `activity_tags`: activity-tag N:M join.
- `activity_images`: activity-image one-to-many.
- `activity_favorites`: user-activity N:M favorite join.
- `activity_reviews`: user-activity review join, unique one review per user/activity.
- `activity_preference_vectors`: activity vector one-to-one.
- `user_preference_vectors`: user vector one-to-one.
- `tag_preference_vectors`: tag vector one-to-one.

## MySQL Migration Notes

- PostgreSQL sequence 없음. 전부 `AUTO_INCREMENT`.
- pgvector 없음. `embedding JSON` + Java cosine similarity.
- PostgreSQL 전용 vector operator `<=>` 없음.
- Flyway MySQL 경로: `classpath:db/migration/mysql`.
- 기존 PostgreSQL migration은 MySQL Flyway 실행 대상 아님.

## DB-Level Constraint Gaps

현재 앱에서만 강제되고 DB `CHECK` 없는 항목:
- 대부분 enum 컬럼 값: `auth_provider`, `role`, `registration_status`, `gender`, `status`, `type`, `tag_group_type`, `activity_type`, `category`, `approval_status`, `position`.
- `embedding` JSON 배열 길이 14, 원소 numeric finite 여부.
- 숫자 범위: `price`, `rating`, `view_count`, `like_count`, `review_count`, `sort_order`, `click_count`.
- 시간 관계: `start_at <= end_at`, `recruit_start_at <= recruit_end_at`.

DB까지 강제 원하면 MySQL `CHECK` 추가 migration 필요.
