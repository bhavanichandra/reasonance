ALTER TABLE spaces
    ADD COLUMN max_turns INT NOT NULL DEFAULT 5,
    ADD COLUMN finalized_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE spaces
    ALTER COLUMN current_turn SET DEFAULT 1;

UPDATE spaces
SET current_turn = 1
WHERE current_turn = 0;

ALTER TABLE nodes
    RENAME COLUMN turn TO generated_turn;

ALTER TABLE nodes
    ADD COLUMN state VARCHAR(50),
    ADD COLUMN completed_turn INT NULL,
    ADD COLUMN used_in_turn INT NULL,
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE nodes
SET state = 'AVAILABLE'
WHERE type IN ('CONCLUSION', 'EVIDENCE');
