CREATE TABLE chat (
  chat_id BIGINT PRIMARY KEY,
  received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  posted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE collected_update (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  actual_till TIMESTAMP WITH TIME ZONE,
  supplier VARCHAR(32) NOT NULL,
  update_key VARCHAR(45) NOT NULL UNIQUE,
  message_json TEXT NOT NULL
);

CREATE INDEX idx_collected_update_actual ON collected_update (actual_till);
CREATE UNIQUE INDEX uniq_collected_update_key ON collected_update (supplier, update_key);

CREATE TABLE distributed_update (
  id BIGSERIAL PRIMARY KEY,
  chat_id BIGINT NOT NULL,
  collected_update_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
