package com.petlog.pet.dto;

import com.petlog.pet.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePetRequest(

    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 50)
    String species,

    @Size(max = 100)
    String breed,

    Gender gender,

    LocalDate birthDate
) {}
