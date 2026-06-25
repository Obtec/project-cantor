package com.cantor.journal;

import com.cantor.journal.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JournalApplicationTests {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void contextLoads() {
        assertThat(tokenProvider).isNotNull();
    }

    @Test
    void jwtRoundTrip() {
        String token = tokenProvider.generateToken(42L, "a@b.com");
        assertThat(tokenProvider.validate(token)).isTrue();
        assertThat(tokenProvider.getUserId(token)).isEqualTo(42L);
    }
}
