CREATE TABLE phone_otp_challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(32) NOT NULL,
    purpose VARCHAR(16) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attempts INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_otp_phone_purpose ON phone_otp_challenges (phone, purpose);
