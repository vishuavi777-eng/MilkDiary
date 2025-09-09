CREATE TABLE IF NOT EXISTS rate_items (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  plan_id        INTEGER NOT NULL,
  species        TEXT NOT NULL,  -- COW/BUFFALO
  type           TEXT NOT NULL,  -- SLAB/FORMULA

  -- slab
  min_fat        NUMERIC,
  max_fat        NUMERIC,
  min_snf        NUMERIC,
  max_snf        NUMERIC,
  rate_per_litre NUMERIC,

  -- formula
  base           NUMERIC,
  per_fat        NUMERIC,
  per_snf        NUMERIC,

  sort_order     INTEGER DEFAULT 0,
  FOREIGN KEY(plan_id) REFERENCES rate_plans(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_rateitem_plan ON rate_items(plan_id);
