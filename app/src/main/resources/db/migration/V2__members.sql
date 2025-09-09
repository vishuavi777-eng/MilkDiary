CREATE TABLE IF NOT EXISTS members (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id  INTEGER NOT NULL,
  code       TEXT NOT NULL,
  name       TEXT NOT NULL,
  address    TEXT,
  phone      TEXT,
  active     INTEGER NOT NULL DEFAULT 1,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(outlet_id) REFERENCES outlets(id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_member_code_outlet ON members(code, outlet_id);
CREATE INDEX IF NOT EXISTS idx_member_outlet ON members(outlet_id);
