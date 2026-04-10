-- Drop ordered-pair rule; use neutral columns + canonical uniqueness (any insert order).
ALTER TABLE friendships DROP CONSTRAINT IF EXISTS chk_friend_order;

CREATE UNIQUE INDEX uq_friendships_canonical_pair
    ON friendships (LEAST(user_low, user_high), GREATEST(user_low, user_high));

ALTER TABLE friendships RENAME COLUMN user_low TO user_one;
ALTER TABLE friendships RENAME COLUMN user_high TO user_two;
