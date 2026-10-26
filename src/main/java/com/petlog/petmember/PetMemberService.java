package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public List<PetMember> getMembersByPet(Pet pet) {
        return petMemberRepository.findByPet(pet);
    }

    public boolean hasAccess(
        User user,
        Pet pet
    ) {
        return petMemberRepository.existsByUserAndPet(
            user,
            pet
        );
    }

    public boolean hasRole(
        User user,
        Pet pet,
        Role role
    ) {
        return petMemberRepository.existsByUserAndPetAndRole(
            user,
            pet,
            role
        );
    }

    public void requireMemberOrOwner(User user, Pet pet) {
        if (!hasRole(user, pet, Role.OWNER)
            && !hasRole(user, pet, Role.MEMBER)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    @Transactional
    public void changeRole(
        User currentUser,
        User targetUser,
        Pet pet,
        Role newRole
    ) {
        if (!hasRole(
            currentUser,
            pet,
            Role.OWNER
        )) {
            throw new AccessDeniedException("Access denied");
        }

        if (newRole == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "New role must not be null"
            );
        }

        PetMember member = petMemberRepository.findByUserAndPet(targetUser, pet)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pet member not found"
            ));

        if (member.getRole() == Role.OWNER || newRole == Role.OWNER ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "OWNER role cannot be changed here"
            );
        }

        member.setRole(newRole);
    }

    @Transactional
    public void deleteMember(
        User currentUser,
        User targetUser,
        Pet pet
    ) {
        if (!hasRole(
            currentUser,
            pet,
            Role.OWNER
        )) {
            throw new AccessDeniedException("Access denied");
        }

        PetMember member = petMemberRepository.findByUserAndPet(targetUser, pet)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pet member not found"
            ));

        if (member.getRole() == Role.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "OWNER cannot be removed"
            );
        }

        petMemberRepository.delete(member);
    }
}
