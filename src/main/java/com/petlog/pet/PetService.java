package com.petlog.pet;

import com.petlog.petmember.PetMemberService;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PetService {

    private final PetRepository petRepository;

    private final PetMemberService petMemberService;

    public PetService(PetRepository petRepository,
                      PetMemberService petMemberService
    ) {
        this.petRepository = petRepository;
        this.petMemberService = petMemberService;
    }

    @Transactional
    public Pet create(
        String name,
        String species,
        User user
    ) {
        Pet pet = new Pet(name, species);
        Pet savedPet = petRepository.save(pet);

        petMemberService.create(user, savedPet, Role.OWNER);

        return savedPet;
    }

    public Pet getById(Long id) {
        return petRepository.findById(id).orElseThrow();
    }

    public Pet getByIdForUser(Long petId, User user) {

        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pet not found"
            ));

        if (!petMemberService.hasAccess(user, pet)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied"
            );
        }

        return pet;
    }
}
