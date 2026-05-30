ALTER TABLE driver
    ADD COLUMN IF NOT EXISTS verification_submitted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE driver
SET verification_submitted = TRUE
WHERE verification_status = 'PENDING'
  AND rc_url IS NOT NULL
  AND license_url IS NOT NULL
  AND insurance_url IS NOT NULL
  AND vehicle_verified = FALSE;
