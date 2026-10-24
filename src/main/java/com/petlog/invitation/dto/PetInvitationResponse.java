package com.petlog.invitation.dto;

import com.petlog.invitation.PetInvitation;
import com.petlog.invitation.PetInvitationStatus;
import com.petlog.petmember.Role;

import java.time.OffsetDateTime;

public record PetInvitationResponse(

    Long id,

    Long petId,

    Long invitedById,

    Long invitedUserId,

    Role role,

    PetInvitationStatus status,

    OffsetDateTime createdAt
) {
    public static PetInvitationResponse from(PetInvitation petInvitation) {
        return new PetInvitationResponse(
            petInvitation.getId(),
            petInvitation.getPet().getId(),
            petInvitation.getInvitedBy().getId(),
            petInvitation.getInvitedUser().getId(),
            petInvitation.getRole(),
            petInvitation.getStatus(),
            petInvitation.getCreatedAt()
        );
    }
}

