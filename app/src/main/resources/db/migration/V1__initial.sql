CREATE TABLE IF NOT EXISTS outlets (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL,
  owner         TEXT,
  address       TEXT,
  phone         TEXT,
  gstin         TEXT,
  created_at    TEXT DEFAULT CURRENT_TIMESTAMP
);
