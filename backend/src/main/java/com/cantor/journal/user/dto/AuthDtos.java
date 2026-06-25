package com.cantor.journal.user.dto;

import com.cantor.journal.user.Role;
import com.cantor.journal.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.stream.Collectors;

public class AuthDtos {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String name,
            String affiliation
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, UserResponse user) {}

    public record UserResponse(
            Long id,
            String email,
            String name,
            String affiliation,
            Set<Role> roles
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getAffiliation(),
                    user.getRoles()
            );
        }
    }

    public record UserSummary(Long id, String name, String email) {
        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getName(), user.getEmail());
        }
    }
}
