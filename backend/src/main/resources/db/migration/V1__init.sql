CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE profiles (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    bio TEXT,
    photo_url VARCHAR(1024),
    tags VARCHAR(512),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE connection_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_request_distinct CHECK (from_user_id <> to_user_id),
    CONSTRAINT uq_request_pair UNIQUE (from_user_id, to_user_id)
);

CREATE INDEX idx_connection_requests_to_status ON connection_requests(to_user_id, status);

CREATE TABLE friendships (
    user_low UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    user_high UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_low, user_high),
    CONSTRAINT chk_friend_order CHECK (user_low < user_high)
);

CREATE TABLE direct_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dm_thread ON direct_messages (created_at DESC);
CREATE INDEX idx_dm_from_to ON direct_messages (from_user_id, to_user_id);
CREATE INDEX idx_dm_to_from ON direct_messages (to_user_id, from_user_id);
