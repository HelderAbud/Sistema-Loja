CREATE TABLE api_idempotency (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scope VARCHAR(32) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_api_idempotency_user_scope_key UNIQUE (user_id, scope, key_hash)
);

CREATE INDEX idx_api_idempotency_user_created ON api_idempotency (user_id, created_at);
