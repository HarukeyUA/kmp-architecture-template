CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    title TEXT NOT NULL,
    done BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX tasks_account_idx ON tasks (account_id);
