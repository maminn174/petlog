CREATE TABLE pet_members (
    user_id BIGINT NOT NULL REFERENCES users(id),
    pet_id BIGINT NOT NULL REFERENCES pets(id),
    role VARCHAR(20) NOT NULL,

    PRIMARY KEY (user_id, pet_id)
);