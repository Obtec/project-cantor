package com.cantor.journal.config;

import com.cantor.journal.user.Role;
import com.cantor.journal.user.User;
import com.cantor.journal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 최초 기동 시 편집자(EDITOR) 시드 계정을 생성한다.
 * 편집자가 리뷰어를 배정하고 게재를 결정하므로, 워크플로우 시작점으로 한 명이 필요하다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.editor-email}")
    private String editorEmail;

    @Value("${app.seed.editor-password}")
    private String editorPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(editorEmail)) {
            return;
        }
        User editor = User.builder()
                .email(editorEmail)
                .passwordHash(passwordEncoder.encode(editorPassword))
                .name("편집장")
                .affiliation("Cantor Journal")
                .roles(Set.of(Role.EDITOR, Role.REVIEWER, Role.AUTHOR))
                .build();
        userRepository.save(editor);
        log.info("시드 편집자 계정 생성: {}", editorEmail);
    }
}
