package com.petlog.invitation.controller;

import com.petlog.invitation.PetInvitation;
import com.petlog.invitation.PetInvitationService;
import com.petlog.invitation.dto.PetInvitationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationActionController {

    private final PetInvitationService petInvitationService;

    public InvitationActionController(PetInvitationService petInvitationService) {
        this.petInvitationService = petInvitationService;
    }

    @PostMapping("/{id}/accept")
    public PetInvitationResponse accept(
        @PathVariable Long id,
        Authentication authentication
    ) {
        String email = authentication.getName();

        PetInvitation invitation = petInvitationService.accept(
            id,
            email
        );

        return PetInvitationResponse.from(invitation);
    }

    @PostMapping("/{id}/decline")
    public PetInvitationResponse decline(
        @PathVariable Long id,
        Authentication authentication
    ) {
        String email = authentication.getName();

        PetInvitation invitation = petInvitationService.decline(
            id,
            email
        );

        return PetInvitationResponse.from(invitation);
    }

    @GetMapping
    public List<PetInvitationResponse> getPendingInvitations(
        Authentication authentication
    ) {
        String email = authentication.getName();
        List<PetInvitation> invitations = petInvitationService.getPendingInvitations(email);

        return invitations.stream()
            .map(PetInvitationResponse::from)
            .toList();
    }
}
