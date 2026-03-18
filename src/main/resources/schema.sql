ALTER TABLE IF EXISTS activity_participations
    DROP CONSTRAINT IF EXISTS activity_participations_status_check;

ALTER TABLE IF EXISTS activity_participations
    ADD CONSTRAINT activity_participations_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'LEFT', 'REJECTED', 'EXCLUDED', 'BLOCKED'));
