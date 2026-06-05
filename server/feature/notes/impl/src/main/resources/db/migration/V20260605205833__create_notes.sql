CREATE TABLE IF NOT EXISTS notes (id uuid PRIMARY KEY, account_id uuid NOT NULL, "text" TEXT NOT NULL, created_at TIMESTAMP NOT NULL);

CREATE INDEX notes_account_idx ON notes (account_id);

