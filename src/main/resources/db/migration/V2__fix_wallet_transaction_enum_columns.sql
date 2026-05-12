-- ============================================================
-- V2: Fix wallet_transaction enum columns
-- Converts transaction_method and transaction_type from ordinal
-- integers to string values to fix check constraint violation
-- with Razorpay payment methods (CARD, UPI, etc.)
-- ============================================================

DO $$
BEGIN

    -- --------------------------------------------------------
    -- Fix transaction_method column (if still integer)
    -- --------------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'wallet_transaction'
          AND column_name = 'transaction_method'
          AND data_type IN ('integer', 'smallint', 'bigint')
    ) THEN
        -- Drop the old ordinal-based check constraint
        ALTER TABLE wallet_transaction
            DROP CONSTRAINT IF EXISTS wallet_transaction_transaction_method_check;

        -- Convert ordinal integers to enum string names
        ALTER TABLE wallet_transaction
            ALTER COLUMN transaction_method TYPE VARCHAR(50)
            USING CASE transaction_method
                WHEN 0 THEN 'BANKING'
                WHEN 1 THEN 'UPI'
                WHEN 2 THEN 'CARD'
                WHEN 3 THEN 'NETBANKING'
                WHEN 4 THEN 'WALLET'
                WHEN 5 THEN 'RIDE'
                ELSE 'BANKING'
            END;

        -- Add correct string-based check constraint
        ALTER TABLE wallet_transaction
            ADD CONSTRAINT wallet_transaction_transaction_method_check
            CHECK (transaction_method IN ('BANKING', 'UPI', 'CARD', 'NETBANKING', 'WALLET', 'RIDE'));

        RAISE NOTICE 'Migrated transaction_method column to VARCHAR';
    END IF;

    -- --------------------------------------------------------
    -- Fix transaction_type column (if still integer)
    -- --------------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'wallet_transaction'
          AND column_name = 'transaction_type'
          AND data_type IN ('integer', 'smallint', 'bigint')
    ) THEN
        -- Drop the old ordinal-based check constraint
        ALTER TABLE wallet_transaction
            DROP CONSTRAINT IF EXISTS wallet_transaction_transaction_type_check;

        -- Convert ordinal integers to enum string names
        ALTER TABLE wallet_transaction
            ALTER COLUMN transaction_type TYPE VARCHAR(10)
            USING CASE transaction_type
                WHEN 0 THEN 'CREDIT'
                WHEN 1 THEN 'DEBIT'
                ELSE 'CREDIT'
            END;

        -- Add correct string-based check constraint
        ALTER TABLE wallet_transaction
            ADD CONSTRAINT wallet_transaction_transaction_type_check
            CHECK (transaction_type IN ('CREDIT', 'DEBIT'));

        RAISE NOTICE 'Migrated transaction_type column to VARCHAR';
    END IF;

END $$;
