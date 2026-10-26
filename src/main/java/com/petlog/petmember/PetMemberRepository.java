package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetMemberRepository extends JpaRepository<PetMember, PetMemberId> {

    @EntityGraph(attributePaths = {"pet"})
    List<PetMember> findByUser(User user);

    @EntityGraph(attributePaths = {"user"})
    List<PetMember> findByPet(Pet pet);

    boolean existsByUserAndPet(User user, Pet pet);

    boolean existsByUserAndPetAndRole(
        User user,
        Pet pet,
        Role role
    );

    Optional<PetMember> findByUserAndPet(User user, Pet pet);
}
