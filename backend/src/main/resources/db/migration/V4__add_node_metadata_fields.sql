-- Add new columns to nodes table
ALTER TABLE nodes
ADD COLUMN IF NOT EXISTS confidence VARCHAR(20),
ADD COLUMN IF NOT EXISTS relevance VARCHAR(20),
ADD COLUMN IF NOT EXISTS reasoning_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS references_turns TEXT;

-- Add index for faster queries
CREATE INDEX IF NOT EXISTS idx_nodes_confidence ON nodes(confidence);
CREATE INDEX IF NOT EXISTS idx_nodes_reasoning_type ON nodes(reasoning_type);
