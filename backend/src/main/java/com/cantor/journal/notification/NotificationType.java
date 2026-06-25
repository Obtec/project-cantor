package com.cantor.journal.notification;

public enum NotificationType {
    SUBMISSION_RECEIVED,   // 편집자에게: 새 논문 투고
    REVIEWER_ASSIGNED,     // 리뷰어에게: 심사 배정
    REVIEW_SUBMITTED,      // 편집자에게: 심사 제출됨
    DECISION_MADE,         // 저자에게: 게재 결정
    REVISION_REQUESTED,    // 저자에게: 수정 요청
    RESUBMITTED,           // 편집자에게: 수정본 재투고
    AUTHOR_REQUEST,        // 편집자에게: 저자의 수정/삭제 요청
    REQUEST_RESOLVED,      // 저자에게: 요청 처리 결과
    ASSIGNMENT_CANCELLED,  // 리뷰어에게: 심사 배정 취소
    EDITOR_GRANTED         // 사용자에게: 편집장 권한 부여
}
