CREATE TABLE IF NOT EXISTS daily_entries (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id       INTEGER NOT NULL,
  member_id       INTEGER NOT NULL,
  date            TEXT NOT NULL,  -- ISO LocalDate
  session         TEXT NOT NULL,  -- AM/PM/BOTH
  species         TEXT NOT NULL,  -- COW/BUFFALO

  qty_litre       NUMERIC NOT NULL,
  fat_pct         NUMERIC,
  snf_pct         NUMERIC,

  rate_applied    NUMERIC,
  amount          NUMERIC,
  rate_item_id    INTEGER,

  notes           TEXT,
  created_at      TEXT DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY(outlet_id) REFERENCES outlets(id) ON DELETE RESTRICT,
  FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE RESTRICT,
  FOREIGN KEY(rate_item_id) REFERENCES rate_items(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_entry_outlet_date ON daily_entries(outlet_id, date);
CREATE INDEX IF NOT EXISTS idx_entry_member_date ON daily_entries(member_id, date);
