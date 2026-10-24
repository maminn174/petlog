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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetInvitationController.class)
public class PetInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetInvitationService petInvitationService;

    @Test
    @WithMockUser(username = "nikita@example.com")
    void invite_shouldReturnCreated() throws Exception {

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
        when(invitation.getStatus()).thenReturn(PetInvitationStatus.PENDING);

        when(petInvitationService.invite(
            5L,
            "nikita@example.com",
            "friend@example.com",
            Role.MEMBER
        )).thenReturn(invitation);

        mockMvc.perform(post("/api/pets/5/invitations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
            {
              "invitedUserEmail": "friend@example.com",
              "role": "MEMBER"
            }
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.petId").value(5))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(petInvitationService).invite(
            5L,
            "nikita@example.com",
            "friend@example.com",
            Role.MEMBER
        );
    }

    @Test
    @WithMockUser(username = "nikita@example.com")
    void invite_shouldReturnBadRequestForInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/pets/5/invitations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
            {
              "invitedUserEmail": "hello",
              "role": "MEMBER"
            }
            """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(petInvitationService);
    }
}
