-- Add per-litre savings columns to outlets and monthly bills
ALTER TABLE outlets ADD COLUMN cow_saving_pl NUMERIC;
ALTER TABLE outlets ADD COLUMN buffalo_saving_pl NUMERIC;
ALTER TABLE monthly_bills ADD COLUMN savings_amount NUMERIC;
UPDATE monthly_bills SET savings_amount = 0 WHERE savings_amount IS NULL;