-- Create cap_locks & audit_log if they don't exist (idempotent)
CREATE TABLE IF NOT EXISTS cap_locks (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id  INTEGER NOT NULL,
  year       INTEGER NOT NULL,
  month      INTEGER NOT NULL,
  cap_no     INTEGER NOT NULL CHECK (cap_no IN (1,2,3)),
  locked_by  TEXT,
  locked_at  TEXT,
  UNIQUE(outlet_id, year, month, cap_no)
);

CREATE INDEX IF NOT EXISTS idx_cap_locks_lookup
  ON cap_locks(outlet_id, year, month, cap_no);

CREATE TABLE IF NOT EXISTS audit_log (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  ts         TEXT NOT NULL,
  user       TEXT,
  action     TEXT NOT NULL,
  outlet_id  INTEGER,
  member_id  INTEGER,
  year       INTEGER,
  month      INTEGER,
  cap_no     INTEGER,
  details    TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_log(ts);
