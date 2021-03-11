ALTER TABLE room
    ADD COLUMN settings_json TEXT;

ALTER TABLE room
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE collected_update
    ADD COLUMN messages_json TEXT;
