DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'app_user'
    ) THEN
        ALTER TABLE app_user ADD COLUMN IF NOT EXISTS refresh_token TEXT;
        ALTER TABLE app_user ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
        ALTER TABLE app_user ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'ride' AND column_name = 'fare'
    ) THEN
        ALTER TABLE ride ALTER COLUMN fare TYPE NUMERIC(19, 2) USING fare::numeric(19,2);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'ride_request' AND column_name = 'fare'
    ) THEN
        ALTER TABLE ride_request ALTER COLUMN fare TYPE NUMERIC(19, 2) USING fare::numeric(19,2);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'wallet' AND column_name = 'balance'
    ) THEN
        ALTER TABLE wallet ALTER COLUMN balance TYPE NUMERIC(19, 2) USING balance::numeric(19,2);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'wallet_transaction' AND column_name = 'amount'
    ) THEN
        ALTER TABLE wallet_transaction ALTER COLUMN amount TYPE NUMERIC(19, 2) USING amount::numeric(19,2);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'payment' AND column_name = 'amount'
    ) THEN
        ALTER TABLE payment ALTER COLUMN amount TYPE NUMERIC(19, 2) USING amount::numeric(19,2);
    END IF;
END $$;
