-- Convert numeric TEXT or INTEGER epoch-millis -> 'YYYY-MM-DD' in rate_plans
UPDATE rate_plans
SET start_date = strftime('%Y-%m-%d', CAST(start_date AS INTEGER)/1000, 'unixepoch')
WHERE start_date IS NOT NULL
  AND (typeof(start_date) = 'integer' OR trim(start_date) GLOB '[0-9]*');

UPDATE rate_plans
SET end_date = strftime('%Y-%m-%d', CAST(end_date AS INTEGER)/1000, 'unixepoch')
WHERE end_date IS NOT NULL
  AND (typeof(end_date) = 'integer' OR trim(end_date) GLOB '[0-9]*');

-- Do the same for other date columns that might have numeric millis:

-- daily_entries.date (LocalDate)
UPDATE daily_entries
SET date = strftime('%Y-%m-%d', CAST(date AS INTEGER)/1000, 'unixepoch')
WHERE date IS NOT NULL
  AND (typeof(date) = 'integer' OR trim(date) GLOB '[0-9]*');

-- monthly_bills.generated_on (LocalDateTime)
UPDATE monthly_bills
SET generated_on = strftime('%Y-%m-%d %H:%M:%S', CAST(generated_on AS INTEGER)/1000, 'unixepoch')
WHERE generated_on IS NOT NULL
  AND (typeof(generated_on) = 'integer' OR trim(generated_on) GLOB '[0-9]*');
