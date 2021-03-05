INSERT INTO room (received_at, posted_at, provider, provider_room_key)
SELECT received_at, posted_at, 'TELEGRAM', chat_id::TEXT
FROM chat
WHERE chat_id::TEXT NOT IN (
    SELECT r1.provider_room_key
    FROM room r1
    WHERE r1.provider = 'TELEGRAM'
);

UPDATE distributed_update u
SET room_id = r.id
FROM room r
WHERE u.room_id IS NULL AND r.provider_room_key = u.chat_id::TEXT;

DROP TABLE chat;

ALTER TABLE distributed_update
    DROP COLUMN chat_id;

ALTER TABLE distributed_update
    ALTER COLUMN room_id SET NOT NULL;
