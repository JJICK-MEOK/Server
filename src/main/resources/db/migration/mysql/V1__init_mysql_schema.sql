CREATE TABLE regions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    parent_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    depth VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_regions_parent FOREIGN KEY (parent_id) REFERENCES regions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    email VARCHAR(150) NULL,
    password_hash VARCHAR(200) NULL,
    auth_provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(50) NULL,
    role VARCHAR(20) NOT NULL,
    registration_status VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_provider UNIQUE (auth_provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    tag_group_type VARCHAR(50) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tags_name_type UNIQUE (name, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE advertisements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    title VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    redirect_url VARCHAR(500) NOT NULL,
    position VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL,
    start_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    view_count INT NOT NULL,
    click_count INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE raw_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    source_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(100) NULL,
    request_url VARCHAR(1000) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT raw_activities_source_type_check CHECK (source_type IN (
        'KOPIS', 'EXHIBITION', 'SEOUL_CULTURE', 'SEOUL_RESERVATION', 'DISCOVERY', 'URL_MANUAL'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    service_terms_agreed BOOLEAN NOT NULL,
    privacy_policy_agreed BOOLEAN NOT NULL,
    marketing_agreed BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_user_profiles_nickname UNIQUE (nickname),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_onboardings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboardings_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_onboardings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_onboarding_regions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_onboarding_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboarding_regions_onboarding_region UNIQUE (user_onboarding_id, region_id),
    CONSTRAINT fk_user_onboarding_regions_onboarding FOREIGN KEY (user_onboarding_id)
        REFERENCES user_onboardings (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_onboarding_regions_region FOREIGN KEY (region_id)
        REFERENCES regions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_onboarding_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_onboarding_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboarding_tags_onboarding_tag UNIQUE (user_onboarding_id, tag_id),
    CONSTRAINT fk_user_onboarding_tags_onboarding FOREIGN KEY (user_onboarding_id)
        REFERENCES user_onboardings (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_onboarding_tags_tag FOREIGN KEY (tag_id)
        REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    region_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    thumbnail_url TEXT NULL,
    source_url TEXT NOT NULL,
    address TEXT NULL,
    organizer TEXT NULL,
    contact_info TEXT NULL,
    target TEXT NULL,
    start_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    recruit_start_at DATETIME(6) NULL,
    recruit_end_at DATETIME(6) NULL,
    price INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    external_id TEXT NULL,
    approval_status VARCHAR(50) NOT NULL,
    view_count INT NOT NULL,
    like_count INT NOT NULL,
    review_count INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activities_region FOREIGN KEY (region_id) REFERENCES regions (id),
    CONSTRAINT activities_source_type_check CHECK (source_type IN (
        'KOPIS', 'EXHIBITION', 'SEOUL_CULTURE', 'SEOUL_RESERVATION', 'DISCOVERY', 'URL_MANUAL'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    activity_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_tags_activity_tag UNIQUE (activity_id, tag_id),
    CONSTRAINT fk_activity_tags_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    activity_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    is_thumbnail BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_images_activity_sort_order UNIQUE (activity_id, sort_order),
    CONSTRAINT fk_activity_images_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_favorites_user_activity UNIQUE (user_id, activity_id),
    CONSTRAINT fk_activity_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_favorites_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    rating INT NOT NULL,
    reason TEXT NULL,
    like_count INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_reviews_user_activity UNIQUE (user_id, activity_id),
    CONSTRAINT fk_activity_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_reviews_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_preference_vectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    embedding JSON NOT NULL,
    vector_version INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_preference_vectors_activity_id UNIQUE (activity_id),
    CONSTRAINT fk_activity_preference_vectors_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_preference_vectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    embedding JSON NOT NULL,
    vector_version INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_preference_vectors_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_preference_vectors_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tag_preference_vectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tag_id BIGINT NOT NULL,
    embedding JSON NOT NULL,
    vector_version INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_preference_vectors_tag_id UNIQUE (tag_id),
    CONSTRAINT fk_tag_preference_vectors_tag FOREIGN KEY (tag_id)
        REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
