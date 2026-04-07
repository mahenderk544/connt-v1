-- Replace email login with normalized mobile number (see PhoneNormalizer in app code).
-- Existing rows get placeholder phones; users should re-register or update numbers in dev.
ALTER TABLE app_users ADD COLUMN phone VARCHAR(32);

UPDATE app_users SET phone = 'migrated_' || replace(id::text, '-', '');

ALTER TABLE app_users ALTER COLUMN phone SET NOT NULL;

ALTER TABLE app_users DROP COLUMN email;

ALTER TABLE app_users ADD CONSTRAINT uq_app_users_phone UNIQUE (phone);
