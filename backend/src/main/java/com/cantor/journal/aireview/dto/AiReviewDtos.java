package com.cantor.journal.aireview.dto;

import com.cantor.journal.aireview.AiReview;
import com.cantor.journal.review.Recommendation;

import java.time.Instant;
import java.util.List;

public class AiReviewDtos {

    public record AiReviewResponse(
            Long id,
            String status,
            String model,
            Recommendation recommendation,
            Integer score,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            String commentsToAuthor,
            String errorMessage,
            Instant createdAt,
            Instant completedAt
    ) {
        public static AiReviewResponse from(AiReview r) {
            return new AiReviewResponse(
                    r.getId(),
                    r.getStatus().name(),
                    r.getModel(),
                    r.getRecommendation(),
                    r.getScore(),
                    r.getSummary(),
                    splitLines(r.getStrengths()),
                    splitLines(r.getWeaknesses()),
                    r.getCommentsToAuthor(),
                    r.getErrorMessage(),
                    r.getCreatedAt(),
                    r.getCompletedAt());
        }

        private static List<String> splitLines(String text) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            return text.lines().filter(s -> !s.isBlank()).toList();
        }
    }
}
