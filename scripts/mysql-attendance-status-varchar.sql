-- If attendance.status was created as MySQL ENUM('PRESENT','ABSENT'), values like HOLIDAY are rejected.
-- Run once against your institute database (e.g. smartinstitute):

ALTER TABLE attendance MODIFY COLUMN status VARCHAR(32) NOT NULL;
