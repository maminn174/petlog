package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetMemberServiceTest {

    @Mock
    private PetMemberRepository petMemberRepository;

    @InjectMocks
    private PetMemberService petMemberService;

    private final User currentUser = new User("Owner", "owner@example.com", "password");
    private final User targetUser = new User("Target", "target@example.com", "password");
    private final Pet pet = new Pet("Buddy", "DOG");

    private void configureCurrentRole(Role role) {
        when(petMemberRepository.existsByUserAndPetAndRole(currentUser, pet, Role.OWNER))
            .thenReturn(role == Role.OWNER);
    }

    private PetMember configureTargetMember(Role role) {
        PetMember member = new PetMember(targetUser, pet, role);
        when(petMemberRepository.findByUserAndPet(targetUser, pet))
            .thenReturn(Optional.of(member));
        return member;
    }

    @Test
    void ownerCanChangeMemberToViewer() {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(Role.MEMBER);

        petMemberService.changeRole(currentUser, targetUser, pet, Role.VIEWER);

        assertEquals(Role.VIEWER, member.getRole());
    }

    @Test
    void ownerCanChangeViewerToMember() {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(Role.VIEWER);

        petMemberService.changeRole(currentUser, targetUser, pet, Role.MEMBER);

        assertEquals(Role.MEMBER, member.getRole());
    }

    @ParameterizedTest
    @CsvSource({"MEMBER", "VIEWER"})
    void ownerCanChangeRoleToSameRole(Role role) {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(role);

        assertDoesNotThrow(() -> petMemberService.changeRole(currentUser, targetUser, pet, role));

        assertEquals(role, member.getRole());
    }

    @Test
    void memberCannotChangeRole() {
        configureCurrentRole(Role.MEMBER);

        assertThrows(AccessDeniedException.class,
            () -> petMemberService.changeRole(currentUser, targetUser, pet, Role.VIEWER));

        verify(petMemberRepository, never()).findByUserAndPet(any(), any());
    }

    @Test
    void viewerCannotChangeRole() {
        configureCurrentRole(Role.VIEWER);

        assertThrows(AccessDeniedException.class,
            () -> petMemberService.changeRole(currentUser, targetUser, pet, Role.MEMBER));

        verify(petMemberRepository, never()).findByUserAndPet(any(), any());
    }

    @ParameterizedTest
    @CsvSource({
        "MEMBER, OWNER",
        "VIEWER, OWNER",
        "OWNER, MEMBER",
        "OWNER, VIEWER",
        "OWNER, OWNER"
    })
    void cannotChangeOwnerRole(Role currentRole, Role newRole) {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(currentRole);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petMemberService.changeRole(currentUser, targetUser, pet, newRole));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(currentRole, member.getRole());
    }

    @Test
    void cannotChangeRoleWhenMemberNotFound() {
        configureCurrentRole(Role.OWNER);
        when(petMemberRepository.findByUserAndPet(targetUser, pet))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petMemberService.changeRole(currentUser, targetUser, pet, Role.VIEWER));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void ownerCanDeleteMember() {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(Role.MEMBER);

        petMemberService.deleteMember(currentUser, targetUser, pet);

        verify(petMemberRepository).delete(member);
    }

    @Test
    void ownerCanDeleteViewer() {
        configureCurrentRole(Role.OWNER);
        PetMember member = configureTargetMember(Role.VIEWER);

        petMemberService.deleteMember(currentUser, targetUser, pet);

        verify(petMemberRepository).delete(member);
    }

    @Test
    void memberCannotDeleteMember() {
        configureCurrentRole(Role.MEMBER);

        assertThrows(AccessDeniedException.class,
            () -> petMemberService.deleteMember(currentUser, targetUser, pet));

        verify(petMemberRepository, never()).findByUserAndPet(any(), any());
        verify(petMemberRepository, never()).delete(any());
    }

    @Test
    void viewerCannotDeleteMember() {
        configureCurrentRole(Role.VIEWER);

        assertThrows(AccessDeniedException.class,
            () -> petMemberService.deleteMember(currentUser, targetUser, pet));

        verify(petMemberRepository, never()).findByUserAndPet(any(), any());
        verify(petMemberRepository, never()).delete(any());
    }

    @Test
    void cannotDeleteOwner() {
        configureCurrentRole(Role.OWNER);
        configureTargetMember(Role.OWNER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petMemberService.deleteMember(currentUser, targetUser, pet));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(petMemberRepository, never()).delete(any());
    }

    @Test
    void cannotDeleteMemberWhenNotFound() {
        configureCurrentRole(Role.OWNER);
        when(petMemberRepository.findByUserAndPet(targetUser, pet))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petMemberService.deleteMember(currentUser, targetUser, pet));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(petMemberRepository, never()).delete(any());
    }

    @Test
    void cannotChangeRoleToNull() {
        configureCurrentRole(Role.OWNER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petMemberService.changeRole(currentUser, targetUser, pet, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(petMemberRepository, never()).findByUserAndPet(any(), any());
    }
}
