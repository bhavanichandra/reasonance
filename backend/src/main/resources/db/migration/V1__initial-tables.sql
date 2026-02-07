CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE spaces
(
    id           UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    name         VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    current_turn INT          NOT NULL    DEFAULT 0,
    status       VARCHAR(50)  NOT NULL    DEFAULT 'NEW' CHECK (status IN ('NEW', 'IN_PROGRESS', 'CLOSED'))
);

CREATE TABLE documents
(
    id         UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL,
    key        VARCHAR(512) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    space_id   UUID         NOT NULL,
    CONSTRAINT fk_document_space FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE
);

CREATE TABLE nodes
(
    id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type     VARCHAR(50) NOT NULL CHECK (type IN ('EVIDENCE', 'CONCLUSION', 'WHAT_IF', 'FINAL_CONCLUSION', 'SOURCE_SUMMARY')),
    content  TEXT         NOT NULL,
    summary  TEXT,
    turn     INT          NOT NULL,
    space_id UUID         NOT NULL,
    CONSTRAINT fk_node_space FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE
);

CREATE TABLE edges
(
    id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_id  UUID         NOT NULL,
    to_id    UUID         NOT NULL,
    relation VARCHAR(255) NOT NULL,
    CONSTRAINT fk_from_node FOREIGN KEY (from_id) REFERENCES nodes (id) ON DELETE CASCADE,
    CONSTRAINT fk_to_node FOREIGN KEY (to_id) REFERENCES nodes (id) ON DELETE CASCADE,
    CONSTRAINT uq_edge_from_to_relation UNIQUE (from_id, to_id, relation)
);
