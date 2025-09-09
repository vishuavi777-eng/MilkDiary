-- Cap-level locks (per outlet, year, month, cap 1/2/3)
CREATE TABLE IF NOT EXISTS cap_locks (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id  INTEGER NOT NULL,
  year       INTEGER NOT NULL,
  month      INTEGER NOT NULL,
  cap_no     INTEGER NOT NULL CHECK (cap_no IN (1,2,3)),
  locked_by  TEXT,
  locked_at  TEXT,                 -- ISO-8601 string
  UNIQUE(outlet_id, year, month, cap_no)
);

CREATE INDEX IF NOT EXISTS idx_cap_locks_lookup
  ON cap_locks(outlet_id, year, month, cap_no);

-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  ts         TEXT NOT NULL,        -- ISO-8601 string
  user       TEXT,
  action     TEXT NOT NULL,        -- e.g., 'generate_bills', 'lock_cap', 'unlock_cap', 'export_cap_pdf'
  outlet_id  INTEGER,
  member_id  INTEGER,
  year       INTEGER,
  month      INTEGER,
  cap_no     INTEGER,
  details    TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_log(ts);
