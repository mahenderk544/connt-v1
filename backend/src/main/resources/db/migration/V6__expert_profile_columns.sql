-- Discoverable "expert" listings (mock ratings / online until real presence & reviews exist).
ALTER TABLE profiles
    ADD COLUMN expert_listed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expert_title VARCHAR(160),
    ADD COLUMN expert_languages VARCHAR(512),
    ADD COLUMN rate_per_min_paise INTEGER,
    ADD COLUMN average_rating NUMERIC(3, 2),
    ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN age_years SMALLINT,
    ADD COLUMN expert_star_featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expert_online_hint BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expert_categories VARCHAR(256);

CREATE INDEX idx_profiles_expert_listed ON profiles (expert_listed) WHERE expert_listed = TRUE;
