package com.cantor.journal.issue;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.issue.dto.IssueDtos.CreateIssueRequest;
import com.cantor.journal.issue.dto.IssueDtos.IssueDetail;
import com.cantor.journal.issue.dto.IssueDtos.IssueResponse;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperRepository;
import com.cantor.journal.paper.PaperService;
import com.cantor.journal.paper.PaperStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueRepository issueRepository;
    private final PaperRepository paperRepository;
    private final PaperService paperService;

    @GetMapping
    public List<IssueResponse> list() {
        return issueRepository.findAllByOrderByYearDescVolumeDescNumberDesc().stream()
                .map(i -> IssueResponse.from(i, paperRepository.countByIssueId(i.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public IssueDetail detail(@PathVariable Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("호를 찾을 수 없습니다."));
        var articles = paperService.responses(paperService.papersInIssue(id), null);
        return new IssueDetail(IssueResponse.from(issue, articles.size()), articles);
    }

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Transactional
    public IssueResponse create(@Valid @RequestBody CreateIssueRequest req) {
        if (issueRepository.existsByVolumeAndNumber(req.volume(), req.number())) {
            throw ApiException.conflict("이미 존재하는 권/호입니다.");
        }
        Issue issue = issueRepository.save(Issue.builder()
                .volume(req.volume())
                .number(req.number())
                .year(req.year())
                .title(req.title())
                .published(false)
                .build());
        return IssueResponse.from(issue, 0);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    @Transactional
    public IssueResponse update(@PathVariable Long id, @Valid @RequestBody CreateIssueRequest req) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("호를 찾을 수 없습니다."));
        boolean numberChanged = issue.getVolume() != req.volume() || issue.getNumber() != req.number();
        if (numberChanged && issueRepository.existsByVolumeAndNumber(req.volume(), req.number())) {
            throw ApiException.conflict("이미 존재하는 권/호입니다.");
        }
        issue.setVolume(req.volume());
        issue.setNumber(req.number());
        issue.setYear(req.year());
        issue.setTitle(req.title());
        issueRepository.save(issue);
        return IssueResponse.from(issue, paperRepository.countByIssueId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("호를 찾을 수 없습니다."));
        // 이 호에 수록됐던 논문은 발행 해제(게재 → 게재확정)하고 수록 정보를 비운다.
        for (Paper p : paperRepository.findByIssueIdOrderByPageStartAsc(id)) {
            p.setIssue(null);
            p.setPageStart(null);
            p.setPageEnd(null);
            p.setPublishedAt(null);
            if (p.getStatus() == PaperStatus.PUBLISHED) {
                p.setStatus(PaperStatus.ACCEPTED);
            }
            paperRepository.save(p);
        }
        issueRepository.delete(issue);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('EDITOR')")
    @Transactional
    public IssueResponse publish(@PathVariable Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("호를 찾을 수 없습니다."));
        issue.setPublished(true);
        issue.setPublishedDate(LocalDate.now());
        issueRepository.save(issue);
        return IssueResponse.from(issue, paperRepository.countByIssueId(id));
    }
}
