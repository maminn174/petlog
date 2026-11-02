package com.petlog.pet.dto;

import com.petlog.pet.Gender;
import com.petlog.pet.Pet;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PetResponse(
    Long id,
    String name,
    String species,
    String breed,
    LocalDate birthDate,
    Gender gender,
    String photo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getBreed(),
            pet.getBirthDate(),
            pet.getGender(),
            pet.getPhoto(),
            pet.getCreatedAt(),
            pet.getUpdatedAt()
        );
    }
}
