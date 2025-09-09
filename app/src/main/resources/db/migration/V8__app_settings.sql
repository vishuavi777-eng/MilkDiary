CREATE TABLE IF NOT EXISTS app_settings (
  key   TEXT PRIMARY KEY,
  value TEXT
);

-- Defaults (insert only if missing)
INSERT OR IGNORE INTO app_settings(key,value) VALUES
 ('printed_by','Operator'),
 ('pdf_fill_missing_days','true'),
 ('pdf_gap_pt','8'),
 ('default_cap','1'),
 ('lock_on_generate','false'),
 ('billing_round_mode','HALF_UP'),
 ('billing_round_to','1'),                -- round to nearest 1.00
 ('backup_dir','backups'),
 ('backups_keep','14');
