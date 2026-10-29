package com.petlog.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(
        Long petId,
        Pageable pageable
    );

    Optional<Event> findByDeletedAtIsNullAndIdAndPet_Id(
        Long eventId,
        Long petId
    );

    @Query(
        value = """
            SELECT *
            FROM events
            WHERE pet_id = :petId
                AND type = 'WALK'
                AND data->>'status' = 'ACTIVE'
                AND deleted_at IS NULL
            """,
        nativeQuery = true)
    Optional<Event> findActiveWalkByPetId(
        @Param("petId") Long petId
    );

    Optional<Event> findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
        Long petId,
        EventType type
    );

    @Query(value = """
    SELECT *
    FROM events
    WHERE pet_id = :petId
      AND type = 'WALK'
      AND deleted_at IS NULL
      AND data->>'status' = 'FINISHED'
      AND occurred_at >= :startOfDay
      AND occurred_at < :endOfDay
    """, nativeQuery = true)
    List<Event> findFinishedWalksForPeriod(
        @Param("petId") Long petId,
        @Param("startOfDay") OffsetDateTime startOfDay,
        @Param("endOfDay") OffsetDateTime endOfDay
    );
}
