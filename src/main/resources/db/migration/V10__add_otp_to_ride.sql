-- Flyway migration to add missing otp column to ride table
-- (ride_request.otp added in V13 for DBs created before V1 included that column)
ALTER TABLE ride ADD COLUMN IF NOT EXISTS otp VARCHAR(255);
