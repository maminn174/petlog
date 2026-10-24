package com.petlog.invitation.dto;

import com.petlog.petmember.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePetInvitationRequest(

    @NotBlank
    @Email
    String invitedUserEmail,

    @NotNull
    Role role
) {
}
