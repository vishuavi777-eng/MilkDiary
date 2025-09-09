-- 1) Add species column to members if missing
ALTER TABLE members ADD COLUMN species TEXT;

-- 2) Backfill species from most frequent daily_entries per member
UPDATE members
SET species = (
  SELECT species
  FROM daily_entries e
  WHERE e.member_id = members.id AND e.species IS NOT NULL
  GROUP BY species
  ORDER BY COUNT(*) DESC
  LIMIT 1
)
WHERE species IS NULL;

-- 3) Default remaining NULLs to 'COW'
UPDATE members SET species = 'COW' WHERE species IS NULL;

-- 4) Make all existing daily_entries match the member’s species
UPDATE daily_entries
SET species = (
  SELECT m.species FROM members m WHERE m.id = daily_entries.member_id
)
WHERE species IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM members m
    WHERE m.id = daily_entries.member_id
      AND m.species IS NOT NULL
      AND m.species <> daily_entries.species
  );

-- 5) Trigger to prevent mismatches going forward
DROP TRIGGER IF EXISTS trg_daily_entries_species_match_ins;
DROP TRIGGER IF EXISTS trg_daily_entries_species_match_upd;

CREATE TRIGGER trg_daily_entries_species_match_ins
BEFORE INSERT ON daily_entries
FOR EACH ROW
BEGIN
  SELECT CASE
    WHEN (SELECT species FROM members WHERE id = NEW.member_id) <> NEW.species
    THEN RAISE(ABORT, 'daily_entries.species must match members.species')
  END;
END;

CREATE TRIGGER trg_daily_entries_species_match_upd
BEFORE UPDATE ON daily_entries
FOR EACH ROW
BEGIN
  SELECT CASE
    WHEN (SELECT species FROM members WHERE id = NEW.member_id) <> NEW.species
    THEN RAISE(ABORT, 'daily_entries.species must match members.species')
  END;
END;
