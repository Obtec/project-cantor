package com.cantor.journal.aireview;

import com.cantor.journal.paper.Paper;
import com.cantor.journal.review.Recommendation;
import com.cantor.journal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AI(Gemini)가 생성한 피어리뷰. 편집자의 심사 보조용 참고 자료이며
 * 정식 심사(Review)와는 별도로 관리한다.
 */
@Entity
@Table(name = "ai_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;

    /** 리뷰 생성에 사용한 모델 ID (예: gemini-2.5-flash) */
    @Column(length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiReviewStatus status;

    @Enumerated(EnumType.STRING)
    private Recommendation recommendation;

    /** 1~5 */
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 줄바꿈으로 구분된 항목 목록 */
    @Column(columnDefinition = "TEXT")
    private String strengths;

    /** 줄바꿈으로 구분된 항목 목록 */
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String commentsToAuthor;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = AiReviewStatus.PENDING;
        }
    }
}
