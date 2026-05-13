-- Allow storing RAZORPAY (Card/UPI) on legacy schemas: CHECK constraints, short
-- VARCHAR, or PostgreSQL enum columns that only listed WALLET/CASH.

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname AS cname, rel.relname AS tname
        FROM pg_constraint con
        JOIN pg_class rel ON con.conrelid = rel.oid
        WHERE con.contype = 'c'
          AND rel.relname IN ('ride_request', 'ride', 'payment')
          AND pg_get_constraintdef(con.oid) ILIKE '%payment_method%'
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', r.tname, r.cname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ride_request'
          AND column_name = 'payment_method'
    ) THEN
        ALTER TABLE ride_request
            ALTER COLUMN payment_method TYPE VARCHAR(50)
            USING (payment_method::text);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ride'
          AND column_name = 'payment_method'
    ) THEN
        ALTER TABLE ride
            ALTER COLUMN payment_method TYPE VARCHAR(50)
            USING (payment_method::text);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'payment'
          AND column_name = 'payment_method'
    ) THEN
        ALTER TABLE payment
            ALTER COLUMN payment_method TYPE VARCHAR(50)
            USING (payment_method::text);
    END IF;
END $$;
