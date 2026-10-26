package com.petlog.petmember.dto;

import com.petlog.petmember.Role;

public record PetMemberResponse(

    Long userId,

    String name,

    Role role
) {
}
