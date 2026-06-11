CREATE TABLE notes (id uuid PRIMARY KEY, account_id uuid NOT NULL, "text" TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL);

CREATE INDEX notes_account_idx ON notes (account_id);
