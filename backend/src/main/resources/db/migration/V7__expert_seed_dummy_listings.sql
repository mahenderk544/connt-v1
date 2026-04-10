-- Enrich seeded test users as sample experts (local / staging when V4 ran).
UPDATE profiles
SET
    display_name = 'Priya',
    bio = 'Relationship & chat support.',
    expert_listed = TRUE,
    expert_title = 'Relationship Expert',
    expert_languages = 'Hindi · Bengali',
    rate_per_min_paise = 500,
    average_rating = 4.30,
    rating_count = 12,
    age_years = 23,
    expert_star_featured = FALSE,
    expert_online_hint = TRUE,
    expert_categories = 'relationship,new',
    tags = 'relationship,new'
WHERE user_id = '10000000-0000-4000-8000-000000000001'::uuid;

UPDATE profiles
SET
    display_name = 'Rashi',
    bio = 'Life coaching and happiness.',
    expert_listed = TRUE,
    expert_title = 'Life Coach',
    expert_languages = 'English · Hindi',
    rate_per_min_paise = 800,
    average_rating = 4.80,
    rating_count = 40,
    age_years = 26,
    expert_star_featured = TRUE,
    expert_online_hint = TRUE,
    expert_categories = 'star,happiness',
    tags = 'star,happiness'
WHERE user_id = '10000000-0000-4000-8000-000000000002'::uuid;
