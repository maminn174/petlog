package com.petlog.invitation;

import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.PetMemberService;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PetInvitationService {

    private final PetInvitationRepository petInvitationRepository;

    private final PetService petService;

    private final UserService userService;

    private final PetMemberService petMemberService;

    public PetInvitationService(
        PetInvitationRepository petInvitationRepository,
        PetService petService,
        UserService userService,
        PetMemberService petMemberService
    ) {
        this.petInvitationRepository = petInvitationRepository;
        this.petService = petService;
        this.userService = userService;
        this.petMemberService = petMemberService;
    }

    @Transactional
    public PetInvitation invite(
        Long petId,
        String senderEmail,
        String invitedUserEmail,
        Role role
    ) {
        if (role == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Role is required"
            );
        }

        User sender = userService.getByEmail(senderEmail);
        Pet pet = petService.getByIdForUser(
            petId,
            sender
        );

        if (!petMemberService.hasRole(
            sender,
            pet,
            Role.OWNER
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only OWNER can invite users"
            );
        }

        User invitedUser = userService.getByEmail(invitedUserEmail);

        if (petMemberService.hasAccess(
            invitedUser,
            pet
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "User is already a member of this pet"
            );
        }

        if (petInvitationRepository.existsByPetAndInvitedUserAndStatus(
            pet,
            invitedUser,
            PetInvitationStatus.PENDING
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Pending invitation already exists"
            );
        }

        PetInvitation petInvitation = new PetInvitation(
            pet,
            sender,
            invitedUser,
            role
        );

        return petInvitationRepository.save(petInvitation);
    }

    @Transactional
    public PetInvitation accept(
        Long invitationId,
        String userEmail
    ) {
        PetInvitation invitation = getPendingInvitationForUser(
            invitationId,
            userEmail
        );


        if (petMemberService.hasAccess(
            invitation.getInvitedUser(),
            invitation.getPet()
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "User is already a member of this pet"
            );
        }

        petMemberService.create(
            invitation.getInvitedUser(),
            invitation.getPet(),
            invitation.getRole()
        );

        invitation.setStatus(PetInvitationStatus.ACCEPTED);

        return invitation;
    }

    @Transactional
    public PetInvitation decline(
        Long invitationId,
        String userEmail
    ) {
        PetInvitation invitation = getPendingInvitationForUser(
            invitationId,
            userEmail
        );

        invitation.setStatus(PetInvitationStatus.DECLINED);

        return invitation;
    }

    private PetInvitation getPendingInvitationForUser(
        Long invitationId,
        String userEmail
    ) {
        PetInvitation invitation = petInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Invitation not found"
            ));

        User user = userService.getByEmail(userEmail);

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This invitation is not for you"
            );
        }

        if (invitation.getStatus() != PetInvitationStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invitation is no longer pending"
            );
        }

        return invitation;
    }

    public List<PetInvitation> getPendingInvitations(
        String userEmail
    ) {
        User user = userService.getByEmail(userEmail);

        return petInvitationRepository.getAllByInvitedUserAndStatus(
            user,
            PetInvitationStatus.PENDING
        );
    }
}
