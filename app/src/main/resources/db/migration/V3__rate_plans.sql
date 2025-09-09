CREATE TABLE IF NOT EXISTS rate_plans (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id  INTEGER NOT NULL,
  name       TEXT NOT NULL,
  start_date TEXT NOT NULL,  -- ISO LocalDate
  end_date   TEXT,           -- nullable
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(outlet_id) REFERENCES outlets(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_rateplan_outlet ON rate_plans(outlet_id);
CREATE INDEX IF NOT EXISTS idx_rateplan_daterange ON rate_plans(start_date, end_date);
