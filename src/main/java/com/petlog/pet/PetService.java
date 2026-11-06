package com.petlog.pet;

import com.petlog.pet.dto.UpdatePetRequest;
import com.petlog.petmember.PetMemberService;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class PetService {

    private final PetRepository petRepository;

    private final PetMemberService petMemberService;

    public PetService(
        PetRepository petRepository,
        PetMemberService petMemberService
    ) {
        this.petRepository = petRepository;
        this.petMemberService = petMemberService;
    }

    @Transactional
    public Pet create(
        String name,
        String species,
        String breed,
        Gender gender,
        LocalDate birthDate,
        User user
    ) {
        Pet pet = new Pet(
            name,
            species
        );

        pet.setBreed(breed);
        pet.setGender(gender);
        pet.setBirthDate(birthDate);

        Pet savedPet = petRepository.save(pet);

        petMemberService.create(
            user,
            savedPet,
            Role.OWNER
        );

        return savedPet;
    }

    @Transactional
    public Pet update(
        Long petId,
        User user,
        UpdatePetRequest request
    ) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pet not found"
            ));

        if (!petMemberService.hasRole(user, pet, Role.OWNER)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.breed() != null) {
            pet.setBreed(request.breed());
        }

        if (request.birthDate() != null) {
            pet.setBirthDate(request.birthDate());
        }

        if (request.gender() != null) {
            pet.setGender(request.gender());
        }

        if (request.name() != null) {
            pet.setName(request.name());
        }

        if (request.photo() != null) {
            pet.setPhoto(request.photo());
        }

        if (request.species() != null) {
            pet.setSpecies(request.species());
        }

        return pet;
    }

    public Pet getById(Long id) {
        return petRepository.findById(id).orElseThrow();
    }

    public Pet getByIdForUser(
        Long petId,
        User user
    ) {

        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pet not found"
            ));

        if (!petMemberService.hasAccess(
            user,
            pet
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied"
            );
        }

        return pet;
    }
}
