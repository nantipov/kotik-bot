UPDATE collected_update SET messages_json = '{"messages":[{"language":"EN","message":' || message_json || '}]}'
WHERE messages_json IS NULL;

ALTER TABLE collected_update ALTER COLUMN message_json SET NOT NULL;

ALTER TABLE collected_update DROP COLUMN message_json;
