package com.cantor.journal.notification;

public enum NotificationType {
    SUBMISSION_RECEIVED,   // 편집자에게: 새 논문 투고
    REVIEWER_ASSIGNED,     // 리뷰어에게: 심사 배정
    REVIEW_SUBMITTED,      // 편집자에게: 심사 제출됨
    DECISION_MADE,         // 저자에게: 게재 결정
    REVISION_REQUESTED,    // 저자에게: 수정 요청
    RESUBMITTED            // 편집자에게: 수정본 재투고
}
