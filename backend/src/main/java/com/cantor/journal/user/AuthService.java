package com.cantor.journal.user;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.security.JwtTokenProvider;
import com.cantor.journal.user.dto.AuthDtos.AuthResponse;
import com.cantor.journal.user.dto.AuthDtos.LoginRequest;
import com.cantor.journal.user.dto.AuthDtos.RegisterRequest;
import com.cantor.journal.user.dto.AuthDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("이미 등록된 이메일입니다.");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .name(req.name())
                .affiliation(req.affiliation())
                .roles(Set.of(Role.AUTHOR))
                .build();
        user = userRepository.save(user);
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
