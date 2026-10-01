-- Existing users may supply an email later; preserve their accounts.
ALTER TABLE app_user ADD COLUMN email VARCHAR(150);
ALTER TABLE app_user ADD CONSTRAINT uq_app_user_email UNIQUE (email);
