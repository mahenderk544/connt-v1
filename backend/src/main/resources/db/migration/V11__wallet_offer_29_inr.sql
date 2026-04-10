-- Example pack: ₹29 → 60 min (adjust or add rows in wallet_topup_offers anytime).
INSERT INTO wallet_topup_offers (id, amount_paise, talk_minutes, sort_order, active, label)
VALUES (uuid_generate_v4(), 2900, 60, 25, TRUE, '₹29 pack')
ON CONFLICT (amount_paise) DO NOTHING;
