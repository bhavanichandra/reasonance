-- Store finalized investigation summary and case status
ALTER TABLE spaces
ADD COLUMN IF NOT EXISTS final_investigation_summary TEXT,
ADD COLUMN IF NOT EXISTS final_case_closed BOOLEAN;
