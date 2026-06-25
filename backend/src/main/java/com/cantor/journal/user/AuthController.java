package com.cantor.journal.user;

import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.dto.AuthDtos.AuthResponse;
import com.cantor.journal.user.dto.AuthDtos.LoginRequest;
import com.cantor.journal.user.dto.AuthDtos.RegisterRequest;
import com.cantor.journal.user.dto.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(principal.getUser());
    }
}
