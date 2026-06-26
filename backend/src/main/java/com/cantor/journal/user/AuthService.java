package com.cantor.journal.user;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.security.JwtTokenProvider;
import com.cantor.journal.user.dto.AuthDtos.AuthResponse;
import com.cantor.journal.user.dto.AuthDtos.LoginRequest;
import com.cantor.journal.user.dto.AuthDtos.RegisterRequest;
import com.cantor.journal.user.dto.AuthDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    // 연속 로그인 실패 N회 시 계정을 일정 시간 잠근다(IP 기반 nginx 제한을 보완).
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다.";

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

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // 임계 초과: 잠그고 카운터는 초기화(잠금 해제 후 다시 N회 기회).
                user.setLockedUntil(now.plus(LOCK_DURATION));
                user.setFailedLoginAttempts(0);
            } else {
                user.setFailedLoginAttempts(attempts);
            }
            userRepository.save(user);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        // 로그인 성공: 실패 기록 초기화.
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
