package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetMemberService {

    private final PetMemberRepository petMemberRepository;

    public PetMemberService(PetMemberRepository petMemberRepository) {
        this.petMemberRepository = petMemberRepository;
    }

    public PetMember create(
        User user,
        Pet pet,
        Role role
    ) {
        PetMember petMember = new PetMember(
            user,
            pet,
            role
        );

        return petMemberRepository.save(petMember);
    }

    public List<Pet> getPetsByUser(User user) {
        List<PetMember> members = petMemberRepository.findByUser(user);

        return members.stream()
            .map(PetMember::getPet)
            .toList();
    }

    public boolean hasAccess(User user, Pet pet) {
        return petMemberRepository.existsByUserAndPet(user, pet);
    }

    public boolean hasRole(
        User user,
        Pet pet,
        Role role
    ) {
        return petMemberRepository.existsByUserAndPetAndRole(user, pet, role);
    }
}
