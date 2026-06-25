package com.cantor.journal.issue.dto;

import com.cantor.journal.issue.Issue;
import com.cantor.journal.paper.dto.PaperDtos.PaperResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public class IssueDtos {

    public record CreateIssueRequest(
            @NotNull @Positive Integer volume,
            @NotNull @Positive Integer number,
            @NotNull Integer year,
            String title
    ) {}

    public record PublishPaperRequest(
            @NotNull Long issueId,
            Integer pageStart,
            Integer pageEnd
    ) {}

    public record IssueResponse(
            Long id,
            int volume,
            int number,
            int year,
            String title,
            String label,
            boolean published,
            LocalDate publishedDate,
            int articleCount
    ) {
        public static IssueResponse from(Issue i, int articleCount) {
            return new IssueResponse(
                    i.getId(), i.getVolume(), i.getNumber(), i.getYear(), i.getTitle(),
                    i.label(), i.isPublished(), i.getPublishedDate(), articleCount);
        }
    }

    public record IssueDetail(
            IssueResponse issue,
            List<PaperResponse> articles
    ) {}
}
