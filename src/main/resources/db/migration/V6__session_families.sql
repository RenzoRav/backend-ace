CREATE TABLE session_family (
 id VARCHAR(36) PRIMARY KEY,
 role VARCHAR(16) NOT NULL,
 account_id BIGINT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 absolute_expires_at TIMESTAMPTZ NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX session_family_account_idx ON session_family(role,account_id);
ALTER TABLE refresh_session ADD COLUMN family_id VARCHAR(36) REFERENCES session_family(id);
CREATE INDEX refresh_session_family_idx ON refresh_session(family_id);
UPDATE refresh_session SET revoked=TRUE WHERE family_id IS NULL;
