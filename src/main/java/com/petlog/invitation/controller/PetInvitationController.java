package com.petlog.invitation.controller;

import com.petlog.invitation.PetInvitation;
import com.petlog.invitation.PetInvitationService;
import com.petlog.invitation.dto.CreatePetInvitationRequest;
import com.petlog.invitation.dto.PetInvitationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets/{petId}/invitations")
public class PetInvitationController {

    private final PetInvitationService petInvitationService;

    public PetInvitationController(PetInvitationService petInvitationService) {
        this.petInvitationService = petInvitationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetInvitationResponse invite(
        @PathVariable Long petId,
        @Valid @RequestBody
        CreatePetInvitationRequest request,
        Authentication authentication
        ) {
        String senderEmail = authentication.getName();

        PetInvitation invitation = petInvitationService.invite(
            petId,
            senderEmail,
            request.invitedUserEmail(),
            request.role()
        );

        return PetInvitationResponse.from(invitation);
    }
}
