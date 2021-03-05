CREATE TABLE room (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    received_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    posted_at         TIMESTAMP WITH TIME ZONE,
    provider          TEXT                     NOT NULL,
    provider_room_key TEXT                     NOT NULL
);

CREATE
UNIQUE INDEX idx_uniq_room_provider_type_key ON room(provider, provider_room_key);

ALTER TABLE distributed_update
    ADD COLUMN room_id BIGINT;

ALTER TABLE distributed_update
    ALTER COLUMN chat_id DROP NOT NULL;

INSERT INTO room (received_at, posted_at, provider, provider_room_key)
SELECT received_at, posted_at, 'TELEGRAM', chat_id::TEXT
FROM chat;

UPDATE distributed_update u
SET room_id = r.id FROM room r
WHERE r.provider_room_key = u.chat_id::TEXT;

