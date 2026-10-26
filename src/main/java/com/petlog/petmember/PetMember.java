package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "pet_members")
public class PetMember {

    @EmbeddedId
    private PetMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("petId")
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    public PetMember() {
    }

    public PetMember(User user, Pet pet, Role role) {
        this.id = new PetMemberId(user.getId(), pet.getId());
        this.user = user;
        this.pet = pet;
        this.role = role;
    }

    public PetMemberId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Pet getPet() {
        return pet;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
