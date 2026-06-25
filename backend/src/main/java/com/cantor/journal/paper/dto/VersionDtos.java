package com.cantor.journal.paper.dto;

import com.cantor.journal.paper.ManuscriptVersion;

import java.time.Instant;

public class VersionDtos {

    public record VersionResponse(
            int versionNo,
            String fileName,
            Long fileSize,
            String responseToReviewers,
            Instant createdAt
    ) {
        public static VersionResponse from(ManuscriptVersion v) {
            return new VersionResponse(
                    v.getVersionNo(), v.getFileName(), v.getFileSize(),
                    v.getResponseToReviewers(), v.getCreatedAt());
        }
    }
}
