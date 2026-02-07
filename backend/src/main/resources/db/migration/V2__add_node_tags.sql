-- Create node_tags table for storing tags
CREATE TABLE node_tags (
    node_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    CONSTRAINT fk_node_tags_node
        FOREIGN KEY (node_id)
        REFERENCES nodes(id)
        ON DELETE CASCADE
);

-- Index for faster tag queries
CREATE INDEX idx_node_tags_node_id ON node_tags(node_id);
CREATE INDEX idx_node_tags_tag ON node_tags(tag);
