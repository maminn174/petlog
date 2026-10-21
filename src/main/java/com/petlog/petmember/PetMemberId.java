package com.petlog.petmember;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PetMemberId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "pet_id")
    private Long petId;

    public PetMemberId() {
    }

    public PetMemberId(Long userId, Long petId) {
        this.userId = userId;
        this.petId = petId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PetMemberId that)) return false;

        return Objects.equals(userId, that.userId)
            && Objects.equals(petId, that.petId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, petId);
    }
}
