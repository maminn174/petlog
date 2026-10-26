package com.petlog.petmember.dto;

import com.petlog.petmember.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(

    @NotNull
    Role role
) {
}
