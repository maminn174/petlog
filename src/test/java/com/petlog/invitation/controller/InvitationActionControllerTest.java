package com.petlog.invitation.controller;

import com.petlog.invitation.PetInvitation;
import com.petlog.invitation.PetInvitationService;
import com.petlog.invitation.PetInvitationStatus;
import com.petlog.pet.Pet;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvitationActionController.class)
public class InvitationActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetInvitationService petInvitationService;

    private static final String TEST_EMAIL = "nikita@example.com";

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void getPendingInvitations_shouldReturnOk() throws Exception {

        when(petInvitationService.getPendingInvitations(TEST_EMAIL))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/invitations"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verify(petInvitationService)
            .getPendingInvitations(TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void getPendingInvitations_shouldReturnInvitationList() throws Exception {

        PetInvitation invitation = createMockInvitation(PetInvitationStatus.PENDING);

        when(petInvitationService.getPendingInvitations(TEST_EMAIL))
            .thenReturn(List.of(invitation));

        mockMvc.perform(get("/api/invitations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].petId").value(5L))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].invitedById").value(2L))
            .andExpect(jsonPath("$[0].invitedUserId").value(3L))
            .andExpect(jsonPath("$[0].role").value("MEMBER"))
            .andExpect(jsonPath("$.length()").value(1));

        verify(petInvitationService)
            .getPendingInvitations(TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void acceptInvitation_shouldReturnOk() throws Exception {

        PetInvitation invitation = createMockInvitation(PetInvitationStatus.ACCEPTED);

        when(petInvitationService.accept(1L, TEST_EMAIL))
            .thenReturn(invitation);

        mockMvc.perform(post("/api/invitations/1/accept")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(petInvitationService).accept(1L, TEST_EMAIL);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void declineInvitation_shouldReturnOk() throws Exception {

        PetInvitation invitation = createMockInvitation(PetInvitationStatus.DECLINED);

        when(petInvitationService.decline(1L, TEST_EMAIL))
            .thenReturn(invitation);

        mockMvc.perform(post("/api/invitations/1/decline")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("DECLINED"));

        verify(petInvitationService).decline(1L, TEST_EMAIL);
    }

    private PetInvitation createMockInvitation(
        PetInvitationStatus status
    ) {
        PetInvitation invitation = mock(PetInvitation.class);
        Pet pet = mock(Pet.class);
        User sender = mock(User.class);
        User invitedUser = mock(User.class);

        when(invitation.getPet()).thenReturn(pet);
        when(invitation.getInvitedBy()).thenReturn(sender);
        when(invitation.getInvitedUser()).thenReturn(invitedUser);

        when(pet.getId()).thenReturn(5L);
        when(sender.getId()).thenReturn(2L);
        when(invitedUser.getId()).thenReturn(3L);
        when(invitation.getId()).thenReturn(1L);
        when(invitation.getRole()).thenReturn(Role.MEMBER);
        when(invitation.getStatus()).thenReturn(status);

        return invitation;
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void acceptInvitation_shouldReturnForbidden() throws Exception {

        when(petInvitationService.accept(1L, TEST_EMAIL))
            .thenThrow(
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This invitation is not for you"
                )
            );

        mockMvc.perform(post("/api/invitations/1/accept")
                .with(csrf()))
            .andExpect(status().isForbidden());

        verify(petInvitationService).accept(1L, TEST_EMAIL);
    }
}
