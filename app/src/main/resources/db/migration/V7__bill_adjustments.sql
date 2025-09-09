CREATE TABLE IF NOT EXISTS bill_adjustments (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  bill_id   INTEGER NOT NULL,
  type      TEXT NOT NULL,   -- ADVANCE/CAN/BONUS/PENALTY/TRANSPORT/OTHER
  amount    NUMERIC NOT NULL,
  remark    TEXT,
  FOREIGN KEY(bill_id) REFERENCES monthly_bills(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_adjust_bill ON bill_adjustments(bill_id);
