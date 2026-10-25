-- AI subtitle tracks wait for an instructor's approval; existing tracks stay READY, as approved.
ALTER TABLE subtitle_track
    ADD COLUMN quality_report       TEXT          NULL,
    ADD COLUMN ai_original_file_key VARCHAR(255)  NULL,
    ADD COLUMN reviewed_by          VARCHAR(100)  NULL,
    ADD COLUMN reviewed_at          DATETIME(6)   NULL,
    ADD COLUMN edited_cue_share     DECIMAL(5, 4) NULL;
