package com.petlog.pet;

import com.petlog.pet.dto.CreatePetRequest;
import com.petlog.pet.dto.PetResponse;
import com.petlog.pet.dto.UpdatePetRequest;
import com.petlog.petmember.PetMemberService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
    name = "Pets",
    description = "Pet profile creation, retrieval and management"
)
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
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse createPet(
        @Valid @RequestBody CreatePetRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        Pet pet = petService.create(
            request.name(),
            request.species(),
            request.breed(),
            request.gender(),
            request.birthDate(),
            user
        );

        return PetResponse.from(pet);
    }

    @GetMapping
    public List<PetResponse> getAll(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());

        List<Pet> pets = petMemberService.getPetsByUser(user);

        return pets.stream()
            .map(PetResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public PetResponse getPetById(
        @PathVariable Long id,
        Authentication authentication
    ) {
        User user = userService.getByEmail(authentication.getName());

        Pet pet = petService.getByIdForUser(
            id,
            user
        );

        return PetResponse.from(pet);
    }

    @PatchMapping("/{id}")
    public PetResponse update(
        @PathVariable Long id,
        Authentication authentication,
        @Valid @RequestBody
        UpdatePetRequest request
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);
        
        Pet pet = petService.update(id, user, request);

        return PetResponse.from(pet);
    }
}
