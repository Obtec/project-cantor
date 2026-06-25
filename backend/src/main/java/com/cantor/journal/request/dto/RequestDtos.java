package com.cantor.journal.request.dto;

import com.cantor.journal.request.PaperRequest;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class RequestDtos {

    public record CreateRequest(
            @NotNull PaperRequest.Type type,
            String message
    ) {}

    public record ResolveRequest(
            @NotNull PaperRequest.Status status,
            String note
    ) {}

    public record RequestResponse(
            Long id,
            Long paperId,
            String paperTitle,
            String requesterName,
            PaperRequest.Type type,
            String message,
            PaperRequest.Status status,
            String editorNote,
            Instant createdAt,
            Instant resolvedAt
    ) {
        public static RequestResponse from(PaperRequest r) {
            return new RequestResponse(
                    r.getId(),
                    r.getPaper().getId(),
                    r.getPaper().getTitle(),
                    r.getRequester().getName(),
                    r.getType(),
                    r.getMessage(),
                    r.getStatus(),
                    r.getEditorNote(),
                    r.getCreatedAt(),
                    r.getResolvedAt()
            );
        }
    }
}
