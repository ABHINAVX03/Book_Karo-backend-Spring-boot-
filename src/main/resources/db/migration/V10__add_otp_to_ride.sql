-- Flyway migration to add missing otp column to ride table
ALTER TABLE ride ADD COLUMN IF NOT EXISTS otp VARCHAR(255);
