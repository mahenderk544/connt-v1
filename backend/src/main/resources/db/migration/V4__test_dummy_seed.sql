-- Local/staging seed data for manual testing (chats, friends, calls).
-- Log in as +15550101001 or +15550101002 with OTP (dev-return-code) to hit these rows.
-- BCrypt hash below matches password "password" (not used for OTP login, but satisfies NOT NULL).

INSERT INTO app_users (id, phone, password_hash, enabled)
VALUES
    ('10000000-0000-4000-8000-000000000001'::uuid, '+15550101001',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', TRUE),
    ('10000000-0000-4000-8000-000000000002'::uuid, '+15550101002',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', TRUE);

INSERT INTO profiles (user_id, display_name, bio)
VALUES
    ('10000000-0000-4000-8000-000000000001'::uuid, 'Dummy Alice', 'Test account'),
    ('10000000-0000-4000-8000-000000000002'::uuid, 'Dummy Bob', 'Test friend');

INSERT INTO friendships (user_low, user_high)
VALUES ('10000000-0000-4000-8000-000000000001'::uuid, '10000000-0000-4000-8000-000000000002'::uuid);

INSERT INTO direct_messages (from_user_id, to_user_id, body)
VALUES (
    '10000000-0000-4000-8000-000000000002'::uuid,
    '10000000-0000-4000-8000-000000000001'::uuid,
    'Dummy row: hello from Bob — use this thread to test voice/video.'
);
