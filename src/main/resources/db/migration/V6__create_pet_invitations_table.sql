CREATE TABLE pet_invitations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES pets(id),
    invited_by BIGINT NOT NULL REFERENCES users(id),
    invited_user BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT NOW(),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
);

CREATE UNIQUE INDEX uq_pet_invitations_pending
ON pet_invitations (pet_id, invited_user)
WHERE status = 'PENDING';