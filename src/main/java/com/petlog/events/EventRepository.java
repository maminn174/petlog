package com.petlog.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
