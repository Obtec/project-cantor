package com.cantor.journal.metric;

import com.cantor.journal.citation.CitationService;
import com.cantor.journal.common.ApiException;
import com.cantor.journal.metric.dto.MetricDtos.AuthorMetricsResponse;
import com.cantor.journal.metric.dto.MetricDtos.AuthorPaper;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperRepository;
import com.cantor.journal.paper.PaperStatus;
import com.cantor.journal.user.User;
import com.cantor.journal.user.UserRepository;
import com.cantor.journal.user.dto.AuthDtos.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final CitationService citationService;
    private final MetricService metricService;

    /**
     * 저자(=제출 계정) 단위 학술 지표. 저자가 제출한 논문들의 피인용수로부터
     * 등록된 모든 지표(피인용수/h-index/g-index 등)를 계산한다.
     * 공개 엔드포인트이므로 게재된 논문만 집계하고 이메일은 노출하지 않는다.
     */
    @GetMapping("/{userId}/metrics")
    public AuthorMetricsResponse metrics(@PathVariable Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("저자를 찾을 수 없습니다."));

        List<Paper> papers = paperRepository.findBySubmitterIdOrderByUpdatedAtDesc(userId).stream()
                .filter(p -> p.getStatus() == PaperStatus.PUBLISHED)
                .toList();
        Map<Long, Long> counts = citationService.citationCounts(
                papers.stream().map(Paper::getId).toList());

        List<Integer> citationCounts = papers.stream()
                .map(p -> counts.getOrDefault(p.getId(), 0L).intValue())
                .toList();

        List<AuthorPaper> authorPapers = papers.stream()
                .map(p -> new AuthorPaper(
                        p.getId(),
                        p.getTitle(),
                        p.getStatus().name(),
                        counts.getOrDefault(p.getId(), 0L)))
                .toList();

        return new AuthorMetricsResponse(
                new UserSummary(author.getId(), author.getName(), null),
                author.getAffiliation(),
                papers.size(),
                metricService.computeAll(citationCounts),
                authorPapers
        );
    }
}
