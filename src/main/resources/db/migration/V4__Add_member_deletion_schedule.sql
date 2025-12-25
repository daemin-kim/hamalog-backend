ALTER TABLE member
    ADD COLUMN deletion_requested_at DATETIME NULL,
    ADD COLUMN deletion_due_at DATETIME NULL,
    ADD COLUMN deletion_scheduled BOOLEAN NOT NULL DEFAULT FALSE;

