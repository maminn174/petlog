package com.petlog.invitation;

import com.petlog.pet.Pet;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pet_invitations")
public class PetInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user", nullable = false)
    private User invitedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PetInvitationStatus status = PetInvitationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public PetInvitation() {
    }

    public PetInvitation(
        Pet pet,
        User invitedBy,
        User invitedUser,
        Role role
    ) {
        this.pet = pet;
        this.invitedBy = invitedBy;
        this.invitedUser = invitedUser;
        this.role = role;
    }

    public Pet getPet() {
        return pet;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public User getInvitedUser() {
        return invitedUser;
    }

    public PetInvitationStatus getStatus() {
        return status;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(PetInvitationStatus status) {
        this.status = status;
    }
}
