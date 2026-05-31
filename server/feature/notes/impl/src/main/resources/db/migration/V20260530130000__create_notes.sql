CREATE TABLE notes (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX notes_account_idx ON notes (account_id);
