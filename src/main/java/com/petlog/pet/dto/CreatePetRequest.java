package com.petlog.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePetRequest(

    @NotBlank
    @Size(max = 100)
    String name,

    @NotBlank
    @Size(max = 50)
    String species
) {}
