-- Create saving periods table
--CREATE TABLE saving_periods (
--    id BIGSERIAL PRIMARY KEY,
--    outlet_id BIGINT NOT NULL REFERENCES outlets(id),
--    start_date DATE NOT NULL,
--    end_date DATE,
--    closed BOOLEAN NOT NULL DEFAULT FALSE
--);
--
--CREATE TABLE IF NOT EXISTS saving_periods (
--  id    INTEGER PRIMARY KEY AUTOINCREMENT,
--  year  INTEGER NOT NULL,
--  month INTEGER NOT NULL,
--  UNIQUE(year, month)
--);

-- Tables for saving periods and member savings
CREATE TABLE saving_periods (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE
);

--CREATE TABLE IF NOT EXISTS member_savings (
--  id                 INTEGER PRIMARY KEY AUTOINCREMENT,
--  saving_period_id   INTEGER NOT NULL,
--  member_id          INTEGER NOT NULL,
--  initial_amount     NUMERIC,
--  accumulated_amount NUMERIC,
--  paid               INTEGER NOT NULL DEFAULT 0,
--  FOREIGN KEY(saving_period_id) REFERENCES saving_periods(id) ON DELETE CASCADE,
--  FOREIGN KEY(member_id)        REFERENCES members(id)       ON DELETE CASCADE
--);
--CREATE UNIQUE INDEX IF NOT EXISTS uk_member_saving_period_member ON member_savings(saving_period_id, member_id);

CREATE TABLE member_savings (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    period_id BIGINT NOT NULL REFERENCES saving_periods(id),
    accumulated_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    paid BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (member_id, period_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_member_saving_period_member ON member_savings(period_id, member_id);
