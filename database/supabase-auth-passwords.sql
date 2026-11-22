-- Supabase Auth owns credentials for both Google and email/password accounts.
-- Existing role records may retain their legacy values.
BEGIN;
ALTER TABLE public.employees ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE public.technicians ALTER COLUMN password_hash DROP NOT NULL;
COMMIT;
