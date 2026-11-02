package com.petlog.auth;

import com.petlog.auth.dto.LoginRequest;
import com.petlog.auth.dto.RegisterRequest;
import com.petlog.auth.dto.UserResponse;
import com.petlog.user.User;
import com.petlog.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Authentication",
    description = "User registration, authentication and session management"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(
        UserService userService,
        AuthService authService
    ) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        User user = userService.create(
            request.name(),
            request.email(),
            request.password()
        );

        UserResponse response = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

    @PostMapping("/login")
    public UserResponse login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response

    ) {
        User user = authService.login(
            loginRequest.email(),
            loginRequest.password(),
            request,
            response
        );

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

    @Operation(
        summary = "Get current user",
        description = "Returns the profile of the currently authenticated user"
    )
    @ApiResponse(
        responseCode = "200",
        description = "User profile retrieved successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Authentication required"
    )
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
