package com.cantor.journal.user;

/**
 * 사용자 역할. Spring Security 권한은 "ROLE_" 접두사가 붙어 사용된다.
 */
public enum Role {
    AUTHOR,
    REVIEWER,
    EDITOR,
    ADMIN
}
