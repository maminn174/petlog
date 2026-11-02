package com.petlog.pet.dto;

import com.petlog.pet.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePetRequest(

    @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
    @Size(max = 100)
    String name,

    @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
    @Size(max = 50)
    String species,

    @Size(max = 100)
    String breed,

    LocalDate birthDate,

    Gender gender,

    String photo
) {
}
