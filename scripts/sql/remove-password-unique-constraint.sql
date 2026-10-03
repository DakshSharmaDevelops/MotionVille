-- Run once on an existing PostgreSQL database with the legacy password constraint.
-- Hibernate ddl-auto=update does not reliably remove old unique constraints.
-- Accounts must have unique usernames/emails, but passwords need not be unique.
-- No user records are changed or deleted by this repair.
DO $$
DECLARE
    password_constraint RECORD;
BEGIN
    FOR password_constraint IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid AND c.conkey = ARRAY[a.attnum]
        WHERE c.conrelid = 'public.app_user'::regclass
          AND c.contype = 'u'
          AND a.attname = 'password'
    LOOP
        EXECUTE format('ALTER TABLE public.app_user DROP CONSTRAINT %I',
                       password_constraint.conname);
    END LOOP;
END $$;
