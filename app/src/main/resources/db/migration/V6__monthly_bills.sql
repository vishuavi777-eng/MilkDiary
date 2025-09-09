CREATE TABLE IF NOT EXISTS monthly_bills (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  outlet_id      INTEGER NOT NULL,
  member_id      INTEGER NOT NULL,
  month          INTEGER NOT NULL,  -- 1..12
  year           INTEGER NOT NULL,

  total_litre    NUMERIC,
  avg_fat        NUMERIC,
  avg_snf        NUMERIC,
  gross_amount   NUMERIC,
  adjustments_total NUMERIC,
  net_amount     NUMERIC,
  round_off      NUMERIC,

  locked         INTEGER NOT NULL DEFAULT 0,
  bill_no        TEXT,
  generated_on   TEXT,

  FOREIGN KEY(outlet_id) REFERENCES outlets(id) ON DELETE RESTRICT,
  FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bill_member_month ON monthly_bills(outlet_id, member_id, month, year);
