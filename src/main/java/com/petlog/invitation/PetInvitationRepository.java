package com.petlog.invitation;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetInvitationRepository extends JpaRepository<PetInvitation, Long> {

    boolean existsByPetAndInvitedUserAndStatus(
        Pet pet,
        User invitedUser,
        PetInvitationStatus status
    );

    List<PetInvitation> getAllByInvitedUserAndStatus(
        User invitedUser,
        PetInvitationStatus status
    );
}
