-- Demo seed data for MilkDiary.
--
-- Usage:
--   sqlite3 app/data/milkdiary.db < scripts/seed-demo.sql
--
-- This script resets business data and creates a deterministic demo dataset
-- for portfolio/testing use. It keeps Flyway metadata intact.

PRAGMA foreign_keys = OFF;

BEGIN TRANSACTION;

DELETE FROM bill_adjustments;
DELETE FROM monthly_bills;
DELETE FROM daily_entries;
DELETE FROM member_savings;
DELETE FROM saving_periods;
DELETE FROM cap_locks;
DELETE FROM audit_log;
DELETE FROM rate_items;
DELETE FROM rate_plans;
DELETE FROM members;
DELETE FROM outlets;

DELETE FROM app_settings
WHERE key IN (
  'active_outlet_id',
  'printed_by',
  'pdf_fill_missing_days',
  'pdf_gap_pt',
  'default_cap',
  'lock_on_generate',
  'billing_round_mode',
  'billing_round_to',
  'backup_dir',
  'backups_keep',
  'ui_lang'
);

INSERT INTO outlets(
  id, name, owner, address, phone, gstin, cow_saving_pl, buffalo_saving_pl
) VALUES (
  1,
  'Portfolio Demo Dairy',
  'Vishwambhar Patil',
  'Karad, Maharashtra',
  '8805705733',
  '27ABCDE1234F1Z5',
  1.00,
  1.50
);

INSERT INTO app_settings(key, value) VALUES
  ('active_outlet_id', '1'),
  ('printed_by', 'Demo Operator'),
  ('pdf_fill_missing_days', 'true'),
  ('pdf_gap_pt', '8'),
  ('default_cap', '1'),
  ('lock_on_generate', 'false'),
  ('billing_round_mode', 'HALF_UP'),
  ('billing_round_to', '1'),
  ('backup_dir', 'backups'),
  ('backups_keep', '14'),
  ('ui_lang', 'en');

INSERT INTO members(id, outlet_id, code, name, address, phone, active, species) VALUES
  (1, 1, 'M001', 'Amit Jadhav',   'Wategaon',       '9000000001', 1, 'COW'),
  (2, 1, 'M002', 'Sunita Patil',  'Yedemachindra',  '9000000002', 1, 'BUFFALO'),
  (3, 1, 'M003', 'Rahul Shinde',  'Islampur',       '9000000003', 1, 'COW'),
  (4, 1, 'M004', 'Meera Kadam',   'Karad',          '9000000004', 1, 'BUFFALO'),
  (5, 1, 'M005', 'Nilesh Pawar',  'Tasgaon',        '9000000005', 1, 'COW'),
  (6, 1, 'M006', 'Anita More',    'Shirala',        '9000000006', 1, 'BUFFALO');

INSERT INTO rate_plans(id, outlet_id, name, start_date, end_date) VALUES
  (1, 1, 'Demo Formula Plan 2026', '2026-01-01', NULL);

INSERT INTO rate_items(
  id, plan_id, species, type, min_fat, max_fat, min_snf, max_snf,
  rate_per_litre, base, per_fat, per_snf, sort_order
) VALUES
  (1, 1, 'COW',     'FORMULA', NULL, NULL, NULL, NULL, NULL, 18.00, 2.20, 1.10, 1),
  (2, 1, 'BUFFALO', 'FORMULA', NULL, NULL, NULL, NULL, NULL, 28.00, 2.80, 1.40, 2);

-- Explicit IDs are used because older SQLite migrations created these tables
-- with BIGSERIAL instead of INTEGER PRIMARY KEY AUTOINCREMENT.
INSERT INTO saving_periods(id, start_date, end_date) VALUES
  (1, '2026-05-01', NULL);

INSERT INTO member_savings(id, member_id, period_id, accumulated_amount, paid) VALUES
  (1, 1, 1, 125.00, 0),
  (2, 2, 1, 210.00, 0),
  (3, 3, 1,  98.00, 0),
  (4, 4, 1, 185.00, 0),
  (5, 5, 1,  75.00, 0),
  (6, 6, 1, 160.00, 0);

-- May 2026 demo collection data. Rates follow:
-- COW     = 18 + fat*2.20 + snf*1.10
-- BUFFALO = 28 + fat*2.80 + snf*1.40
INSERT INTO daily_entries(
  outlet_id, member_id, date, session, species, qty_litre, fat_pct, snf_pct,
  rate_applied, amount, notes
) VALUES
  (1, 1, '2026-05-01', 'AM', 'COW',     8.50, 4.20, 8.50, 36.590, 311.02, 'Demo entry'),
  (1, 2, '2026-05-01', 'AM', 'BUFFALO', 6.00, 6.50, 9.00, 58.800, 352.80, 'Demo entry'),
  (1, 3, '2026-05-01', 'AM', 'COW',     7.25, 4.00, 8.20, 35.820, 259.70, 'Demo entry'),
  (1, 4, '2026-05-01', 'AM', 'BUFFALO', 5.75, 6.20, 8.80, 57.680, 331.66, 'Demo entry'),
  (1, 5, '2026-05-01', 'AM', 'COW',     9.00, 4.40, 8.60, 37.140, 334.26, 'Demo entry'),
  (1, 6, '2026-05-01', 'AM', 'BUFFALO', 6.80, 6.80, 9.20, 59.920, 407.46, 'Demo entry'),

  (1, 1, '2026-05-01', 'PM', 'COW',     7.80, 4.10, 8.40, 36.260, 282.83, 'Demo entry'),
  (1, 2, '2026-05-01', 'PM', 'BUFFALO', 5.40, 6.40, 8.90, 58.380, 315.25, 'Demo entry'),
  (1, 3, '2026-05-01', 'PM', 'COW',     6.90, 3.90, 8.10, 35.490, 244.88, 'Demo entry'),
  (1, 4, '2026-05-01', 'PM', 'BUFFALO', 5.20, 6.10, 8.70, 57.260, 297.75, 'Demo entry'),
  (1, 5, '2026-05-01', 'PM', 'COW',     8.40, 4.30, 8.50, 36.810, 309.20, 'Demo entry'),
  (1, 6, '2026-05-01', 'PM', 'BUFFALO', 6.10, 6.70, 9.10, 59.500, 362.95, 'Demo entry'),

  (1, 1, '2026-05-02', 'AM', 'COW',     8.70, 4.30, 8.60, 36.920, 321.20, 'Demo entry'),
  (1, 2, '2026-05-02', 'AM', 'BUFFALO', 6.30, 6.60, 9.10, 59.220, 373.09, 'Demo entry'),
  (1, 3, '2026-05-02', 'AM', 'COW',     7.10, 4.00, 8.30, 35.930, 255.10, 'Demo entry'),
  (1, 4, '2026-05-02', 'AM', 'BUFFALO', 5.90, 6.30, 8.90, 58.100, 342.79, 'Demo entry'),
  (1, 5, '2026-05-02', 'AM', 'COW',     9.20, 4.50, 8.70, 37.470, 344.72, 'Demo entry'),
  (1, 6, '2026-05-02', 'AM', 'BUFFALO', 6.60, 6.90, 9.20, 60.200, 397.32, 'Demo entry'),

  (1, 1, '2026-05-02', 'PM', 'COW',     7.60, 4.10, 8.30, 36.150, 274.74, 'Demo entry'),
  (1, 2, '2026-05-02', 'PM', 'BUFFALO', 5.70, 6.40, 9.00, 58.520, 333.56, 'Demo entry'),
  (1, 3, '2026-05-02', 'PM', 'COW',     6.80, 3.90, 8.20, 35.600, 242.08, 'Demo entry'),
  (1, 4, '2026-05-02', 'PM', 'BUFFALO', 5.30, 6.20, 8.80, 57.680, 305.70, 'Demo entry'),
  (1, 5, '2026-05-02', 'PM', 'COW',     8.20, 4.20, 8.50, 36.590, 300.04, 'Demo entry'),
  (1, 6, '2026-05-02', 'PM', 'BUFFALO', 6.20, 6.70, 9.00, 59.360, 368.03, 'Demo entry'),

  (1, 1, '2026-05-03', 'AM', 'COW',     8.40, 4.20, 8.40, 36.480, 306.43, 'Demo entry'),
  (1, 2, '2026-05-03', 'AM', 'BUFFALO', 6.10, 6.50, 9.10, 58.940, 359.53, 'Demo entry'),
  (1, 3, '2026-05-03', 'AM', 'COW',     7.00, 4.10, 8.30, 36.150, 253.05, 'Demo entry'),
  (1, 4, '2026-05-03', 'AM', 'BUFFALO', 5.80, 6.20, 8.90, 57.820, 335.36, 'Demo entry'),
  (1, 5, '2026-05-03', 'AM', 'COW',     9.10, 4.40, 8.70, 37.250, 338.98, 'Demo entry'),
  (1, 6, '2026-05-03', 'AM', 'BUFFALO', 6.50, 6.80, 9.30, 60.060, 390.39, 'Demo entry');

-- Prebuilt May 2026 bills so the billing screen has rows immediately.
INSERT INTO monthly_bills(
  outlet_id, member_id, month, year, total_litre, avg_fat, avg_snf,
  gross_amount, adjustments_total, savings_amount, net_amount, round_off,
  locked, bill_no, generated_on
)
SELECT
  1,
  member_id,
  5,
  2026,
  ROUND(SUM(qty_litre), 3),
  ROUND(SUM(fat_pct * qty_litre) / SUM(qty_litre), 2),
  ROUND(SUM(snf_pct * qty_litre) / SUM(qty_litre), 2),
  ROUND(SUM(amount), 2),
  0,
  ROUND(SUM(qty_litre) * CASE species WHEN 'BUFFALO' THEN 1.50 ELSE 1.00 END, 2),
  ROUND(ROUND(SUM(amount), 2) - ROUND(SUM(qty_litre) * CASE species WHEN 'BUFFALO' THEN 1.50 ELSE 1.00 END, 2), 0),
  ROUND(
    ROUND(ROUND(SUM(amount), 2) - ROUND(SUM(qty_litre) * CASE species WHEN 'BUFFALO' THEN 1.50 ELSE 1.00 END, 2), 0)
    - (ROUND(SUM(amount), 2) - ROUND(SUM(qty_litre) * CASE species WHEN 'BUFFALO' THEN 1.50 ELSE 1.00 END, 2)),
    2
  ),
  0,
  'DEMO-2026-05-' || printf('%03d', member_id),
  datetime('now')
FROM daily_entries
WHERE outlet_id = 1 AND date BETWEEN '2026-05-01' AND '2026-05-31'
GROUP BY member_id, species;

INSERT INTO bill_adjustments(bill_id, type, amount, remark)
SELECT id, 'BONUS', 25.00, 'Demo quality bonus'
FROM monthly_bills
WHERE member_id IN (1, 2);

UPDATE monthly_bills
SET adjustments_total = 25.00,
    net_amount = ROUND(gross_amount - savings_amount + 25.00, 0),
    round_off = ROUND(ROUND(gross_amount - savings_amount + 25.00, 0) - (gross_amount - savings_amount + 25.00), 2)
WHERE member_id IN (1, 2);

INSERT INTO cap_locks(outlet_id, year, month, cap_no, locked_by, locked_at)
VALUES (1, 2026, 5, 1, 'Demo Operator', datetime('now'));

COMMIT;

PRAGMA foreign_keys = ON;
