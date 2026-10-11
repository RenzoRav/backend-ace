CREATE TABLE authentication_limit (
 scope VARCHAR(64) NOT NULL,
 key_hash VARCHAR(64) NOT NULL,
 attempts INTEGER NOT NULL,
 window_end TIMESTAMPTZ NOT NULL,
 PRIMARY KEY (scope,key_hash)
);
CREATE INDEX authentication_limit_expiry_idx ON authentication_limit(window_end);
CREATE TABLE security_event (
 id BIGSERIAL PRIMARY KEY,
 event VARCHAR(64) NOT NULL,
 result VARCHAR(32) NOT NULL,
 actor_reference VARCHAR(64) NOT NULL,
 correlation_id VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX security_event_created_idx ON security_event(created_at);
