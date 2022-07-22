CREATE TABLE announcement (
    id SERIAL PRIMARY KEY,
    announcement_markdown TEXT,
    language VARCHAR(2) NOT NULL,
    group_id VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
