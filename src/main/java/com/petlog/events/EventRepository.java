package com.petlog.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDesc(Long petId);

    Optional<Event> findByDeletedAtIsNullAndIdAndPet_Id(
        Long eventId,
        Long petId
    );
}
