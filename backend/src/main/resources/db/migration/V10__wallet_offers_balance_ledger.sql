-- Configurable top-up packs: rupee amount -> included talk minutes (marketing; settlement is amount_paise).
CREATE TABLE wallet_topup_offers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    amount_paise INT NOT NULL CHECK (amount_paise > 0),
    talk_minutes INT NOT NULL CHECK (talk_minutes > 0),
    label VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallet_offer_amount UNIQUE (amount_paise)
);

CREATE INDEX idx_wallet_offers_active_sort ON wallet_topup_offers (active, sort_order);

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS wallet_balance_paise BIGINT NOT NULL DEFAULT 0;

CREATE TABLE wallet_topup_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    offer_id UUID REFERENCES wallet_topup_offers (id) ON DELETE SET NULL,
    amount_paise INT NOT NULL CHECK (amount_paise > 0),
    talk_minutes INT NOT NULL CHECK (talk_minutes > 0),
    status VARCHAR(24) NOT NULL,
    payment_provider VARCHAR(32) NOT NULL,
    external_order_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_wallet_sessions_user_status ON wallet_topup_sessions (user_id, status, created_at DESC);

CREATE TABLE wallet_ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    entry_type VARCHAR(32) NOT NULL,
    amount_paise BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    offer_id UUID REFERENCES wallet_topup_offers (id) ON DELETE SET NULL,
    session_id UUID REFERENCES wallet_topup_sessions (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_ledger_user_created ON wallet_ledger_entries (user_id, created_at DESC);

-- Seed packs: ₹9 → ₹1499 (adjust rows in this table anytime; no code change).
INSERT INTO wallet_topup_offers (id, amount_paise, talk_minutes, sort_order, active, label)
VALUES (uuid_generate_v4(), 900, 20, 10, TRUE, 'Quick chat'),
       (uuid_generate_v4(), 1900, 40, 20, TRUE, 'Starter'),
       (uuid_generate_v4(), 4900, 110, 30, TRUE, 'Value'),
       (uuid_generate_v4(), 9900, 240, 40, TRUE, 'Popular'),
       (uuid_generate_v4(), 14900, 380, 50, TRUE, NULL),
       (uuid_generate_v4(), 19900, 520, 60, TRUE, NULL),
       (uuid_generate_v4(), 29900, 800, 70, TRUE, NULL),
       (uuid_generate_v4(), 39900, 1080, 80, TRUE, NULL),
       (uuid_generate_v4(), 49900, 1380, 90, TRUE, NULL),
       (uuid_generate_v4(), 69900, 1980, 100, TRUE, NULL),
       (uuid_generate_v4(), 99900, 2900, 110, TRUE, NULL),
       (uuid_generate_v4(), 129900, 3800, 120, TRUE, NULL),
       (uuid_generate_v4(), 149900, 4500, 130, TRUE, NULL),
       (uuid_generate_v4(), 150000, 4600, 140, TRUE, '₹1500 max');
