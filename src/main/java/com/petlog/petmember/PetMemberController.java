package com.petlog.petmember;

import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.dto.PetMemberResponse;
import com.petlog.petmember.dto.UpdateRoleRequest;
import com.petlog.user.User;
import com.petlog.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/members")
public class PetMemberController {

    private final PetService petService;
    private final PetMemberService petMemberService;
    private final UserRepository userRepository;

    public PetMemberController(PetService petService,
                               PetMemberService petMemberService,
                               UserRepository userRepository
    ) {
        this.petService = petService;
        this.petMemberService = petMemberService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PetMemberResponse> getAllMembers(
        @PathVariable Long petId,
        Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow();

        Pet pet = petService.getByIdForUser(petId, user);

        List<PetMember> members =  petMemberService.getMembersByPet(pet);

        return members.stream()
            .map(member -> new PetMemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getRole()
            ))
            .toList();
    }

    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
        @PathVariable Long petId,
        @PathVariable Long userId,
        @Valid @RequestBody UpdateRoleRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
            .orElseThrow();

        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
            ));

        Pet pet = petService.getByIdForUser(petId, currentUser);

        Role newRole = request.role();

        petMemberService.changeRole(currentUser, targetUser, pet, newRole);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(
        @PathVariable Long petId,
        @PathVariable Long userId,
        Authentication authentication
    ) {
        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
            .orElseThrow();

        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
            ));

        Pet pet = petService.getByIdForUser(petId, currentUser);

        petMemberService.deleteMember(currentUser, targetUser, pet);
    }
}
