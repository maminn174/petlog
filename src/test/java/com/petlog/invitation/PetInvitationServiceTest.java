package com.petlog.invitation;

import com.petlog.events.exception.EventValidationException;
import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.PetMemberService;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.apache.coyote.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetInvitationServiceTest {

    @Mock
    private PetInvitationRepository petInvitationRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserService userService;

    @Mock
    private PetMemberService petMemberService;

    @InjectMocks
    private PetInvitationService petInvitationService;

    String senderEmail = "nikita@example.com";
    String senderName = "Никита";
    String invitedEmail = "iriska@example.com";
    String invitedName = "Ирисочка";
    String dogName = "Буся";
    String dogSpecies = "DOG";

    @Test
    void invite_shouldCreateInvitation_whenSenderIsOwner() {

        User sender = new User(
            senderName,
            senderEmail,
            "33"

        );

        User invitedUser = new User(
            invitedName,
            invitedEmail,
            "44"

        );

        Pet pet = new Pet(
            dogName,
            dogSpecies
        );

        when(userService.getByEmail(senderEmail))
            .thenReturn(sender);

        when(userService.getByEmail(invitedEmail))
            .thenReturn(invitedUser);

        when(petService.getByIdForUser(1L, sender))
            .thenReturn(pet);

        when(petMemberService.hasRole(sender, pet, Role.OWNER))
            .thenReturn(true);

        PetInvitation invitation = petInvitationService.invite(
            1L,
            senderEmail,
            invitedEmail,
            Role.MEMBER
        );

        ArgumentCaptor<PetInvitation> captor =
            ArgumentCaptor.forClass(PetInvitation.class);

        verify(petInvitationRepository).save(captor.capture());

        PetInvitation savedInvitation = captor.getValue();

        assertEquals(pet, savedInvitation.getPet());
        assertEquals(sender, savedInvitation.getInvitedBy());
        assertEquals(invitedUser, savedInvitation.getInvitedUser());
        assertEquals(Role.MEMBER, savedInvitation.getRole());
        assertEquals(PetInvitationStatus.PENDING, savedInvitation.getStatus());

    }

    @Test
    void invite_shouldThrowForbidden_whenSenderIsNotOwner() {

        User sender = new User(
            senderName,
            senderEmail,
            "33"

        );

        Pet pet = new Pet(
            dogName,
            dogSpecies
        );

        when(userService.getByEmail(senderEmail))
            .thenReturn(sender);

        when(petService.getByIdForUser(1L, sender))
            .thenReturn(pet);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> petInvitationService.invite(
                1L,
                senderEmail,
                invitedEmail,
                Role.MEMBER
            ));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only OWNER can invite users", exception.getReason());

        verify(petInvitationRepository, never())
            .save(any(PetInvitation.class));
    }
}
