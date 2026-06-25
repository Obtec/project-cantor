package com.cantor.journal.metric.dto;

import com.cantor.journal.user.dto.AuthDtos.UserSummary;

import java.util.List;

public class MetricDtos {

    public record MetricValue(
            String key,
            String label,
            String description,
            double value,
            boolean integer
    ) {}

    public record AuthorPaper(
            Long id,
            String title,
            String status,
            long citationCount
    ) {}

    public record AuthorMetricsResponse(
            UserSummary author,
            String affiliation,
            int paperCount,
            List<MetricValue> metrics,
            List<AuthorPaper> papers
    ) {}
}
