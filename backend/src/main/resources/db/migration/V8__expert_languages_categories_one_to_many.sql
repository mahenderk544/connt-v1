-- One profile -> many spoken languages and many expert categories (normalized).
CREATE TABLE expert_profile_languages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles (user_id) ON DELETE CASCADE,
    label VARCHAR(80) NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_expert_lang_user_label UNIQUE (user_id, label)
);

CREATE TABLE expert_profile_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles (user_id) ON DELETE CASCADE,
    category VARCHAR(64) NOT NULL,
    CONSTRAINT uq_expert_cat_user_category UNIQUE (user_id, category)
);

CREATE INDEX idx_expert_lang_user ON expert_profile_languages (user_id);
CREATE INDEX idx_expert_cat_user ON expert_profile_categories (user_id);
CREATE INDEX idx_expert_cat_category_lower ON expert_profile_categories (LOWER(category));

-- Migrate from legacy VARCHAR columns (handles "Hindi · Bengali" and comma lists).
INSERT INTO expert_profile_languages (id, user_id, label, sort_order)
SELECT uuid_generate_v4(),
       p.user_id,
       trim(both from t.lang),
       (t.ord - 1)::smallint
FROM profiles p
         CROSS JOIN LATERAL unnest(
        string_to_array(replace(COALESCE(p.expert_languages, ''), ' · ', ','), ',')
    ) WITH ORDINALITY AS t (lang, ord)
WHERE COALESCE(trim(both from p.expert_languages), '') <> ''
  AND trim(both from t.lang) <> '';

INSERT INTO expert_profile_categories (id, user_id, category)
SELECT uuid_generate_v4(),
       p.user_id,
       lower(trim(both from t.cat))
FROM profiles p
         CROSS JOIN LATERAL unnest(string_to_array(COALESCE(p.expert_categories, ''), ',')) AS t (cat)
WHERE COALESCE(trim(both from p.expert_categories), '') <> ''
  AND trim(both from t.cat) <> '';

ALTER TABLE profiles
    DROP COLUMN IF EXISTS expert_languages,
    DROP COLUMN IF EXISTS expert_categories;
