CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    account_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX sessions_account_idx ON sessions (account_id);

CREATE INDEX sessions_expires_at_idx ON sessions (expires_at);
