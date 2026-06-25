package com.cantor.journal.paper.dto;

import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperStatus;
import com.cantor.journal.user.dto.AuthDtos.UserSummary;

import java.time.Instant;

public class PaperDtos {

    public record PaperResponse(
            Long id,
            String title,
            String abstractText,
            String authorsText,
            String keywords,
            String category,
            PaperStatus status,
            UserSummary submitter,
            String fileName,
            Long fileSize,
            String decisionNote,
            long citationCount,
            String articleCode,
            String issueLabel,
            String pages,
            java.time.Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static PaperResponse from(Paper p) {
            return from(p, 0L);
        }

        public static PaperResponse from(Paper p, long citationCount) {
            String pages = p.getPageStart() == null ? null
                    : (p.getPageEnd() != null ? p.getPageStart() + "–" + p.getPageEnd()
                                              : String.valueOf(p.getPageStart()));
            return new PaperResponse(
                    p.getId(),
                    p.getTitle(),
                    p.getAbstractText(),
                    p.getAuthorsText(),
                    p.getKeywords(),
                    p.getCategory(),
                    p.getStatus(),
                    UserSummary.from(p.getSubmitter()),
                    p.getFileName(),
                    p.getFileSize(),
                    p.getDecisionNote(),
                    citationCount,
                    p.getArticleCode(),
                    p.getIssue() != null ? p.getIssue().label() : null,
                    pages,
                    p.getPublishedAt(),
                    p.getCreatedAt(),
                    p.getUpdatedAt()
            );
        }
    }

    public record DecisionRequest(String decision, String note) {}
}
