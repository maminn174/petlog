CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES pets(id),
    created_by BIGINT NOT NULL REFERENCES users(id),

    type VARCHAR(30) NOT NULL,
    occurred_at timestamptz NOT NULL,
    data jsonb NOT NULL,

    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_pet_occurred
ON events (pet_id, occurred_at DESC);