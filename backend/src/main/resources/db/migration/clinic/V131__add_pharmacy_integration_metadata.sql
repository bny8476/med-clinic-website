-- V131: Add external pharmacy integration metadata to prescriptions table
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS pharmacy_reference_id VARCHAR(255);
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS sent_to_pharmacy_at TIMESTAMP;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS last_pharmacy_status_updated_at TIMESTAMP;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS pharmacy_sync_error VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_prescriptions_pharmacy_ref_id ON prescriptions(pharmacy_reference_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_pharmacy_status ON prescriptions(pharmacy_status);
