package com.petlog.pet;

import com.petlog.pet.dto.CreatePetRequest;
import com.petlog.petmember.PetMemberService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final UserService userService;
    private final PetMemberService petMemberService;

    public PetController(
        PetService petService,
        UserService userService,
        PetMemberService petMemberService
    ) {
        this.petService = petService;
        this.userService = userService;
        this.petMemberService = petMemberService;
    }

    @PostMapping
    public Pet createPet(
        @Valid @RequestBody CreatePetRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        return petService.create(request.name(), request.species(), user);
    }

    @GetMapping
    public List<Pet> getAll(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());

        return petMemberService.getPetsByUser(user);
    }

    @GetMapping("/{id}")
    public Pet getPetById(
        @PathVariable Long id,
        Authentication authentication
    ) {
        User user = userService.getByEmail(authentication.getName());

        return petService.getByIdForUser(id, user);
    }
}
