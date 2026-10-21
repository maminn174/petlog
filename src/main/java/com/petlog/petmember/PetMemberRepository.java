package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetMemberRepository extends JpaRepository<PetMember, PetMemberId> {

    @EntityGraph(attributePaths = {"pet"})
    List<PetMember> findByUser(User user);

    boolean existsByUserAndPet(User user, Pet pet);
}
