package com.cantor.journal.review.dto;

import com.cantor.journal.review.AssignmentStatus;
import com.cantor.journal.review.Recommendation;
import com.cantor.journal.review.Review;
import com.cantor.journal.review.ReviewAssignment;
import com.cantor.journal.user.dto.AuthDtos.UserSummary;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public class ReviewDtos {

    public record AssignRequest(
            @NotNull Long reviewerId,
            LocalDate dueDate
    ) {}

    public record SubmitReviewRequest(
            @NotNull Long assignmentId,
            @NotNull Recommendation recommendation,
            String commentsToEditor,
            String commentsToAuthor,
            Integer score
    ) {}

    public record AssignmentResponse(
            Long id,
            Long paperId,
            String paperTitle,
            UserSummary reviewer,
            AssignmentStatus status,
            LocalDate dueDate,
            boolean reviewSubmitted,
            Instant createdAt
    ) {
        public static AssignmentResponse from(ReviewAssignment a, boolean reviewSubmitted) {
            return new AssignmentResponse(
                    a.getId(),
                    a.getPaper().getId(),
                    a.getPaper().getTitle(),
                    UserSummary.from(a.getReviewer()),
                    a.getStatus(),
                    a.getDueDate(),
                    reviewSubmitted,
                    a.getCreatedAt()
            );
        }
    }

    public record ReviewResponse(
            Long id,
            Long assignmentId,
            Long paperId,
            UserSummary reviewer,
            Recommendation recommendation,
            String commentsToEditor,
            String commentsToAuthor,
            Integer score,
            Instant createdAt
    ) {
        public static ReviewResponse from(Review r) {
            ReviewAssignment a = r.getAssignment();
            return new ReviewResponse(
                    r.getId(),
                    a.getId(),
                    a.getPaper().getId(),
                    UserSummary.from(a.getReviewer()),
                    r.getRecommendation(),
                    r.getCommentsToEditor(),
                    r.getCommentsToAuthor(),
                    r.getScore(),
                    r.getCreatedAt()
            );
        }

        /** 저자에게 보이는 익명 심사 의견: 리뷰어 신원과 편집자 전용 코멘트를 가린다. */
        public static ReviewResponse authorView(Review r, int index) {
            ReviewAssignment a = r.getAssignment();
            return new ReviewResponse(
                    r.getId(),
                    a.getId(),
                    a.getPaper().getId(),
                    new UserSummary(null, "리뷰어 " + index, ""),
                    r.getRecommendation(),
                    null,
                    r.getCommentsToAuthor(),
                    r.getScore(),
                    r.getCreatedAt()
            );
        }
    }
}
