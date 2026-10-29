CREATE UNIQUE INDEX uq_events_active_walk_per_pet
ON events(pet_id)
WHERE type = 'WALK'
    AND data->>'status' = 'ACTIVE'
    AND deleted_at is NULL;