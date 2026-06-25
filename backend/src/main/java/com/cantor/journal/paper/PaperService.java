package com.cantor.journal.paper;

import com.cantor.journal.citation.CitationService;
import com.cantor.journal.citation.ReferenceExtractionService;
import com.cantor.journal.common.ApiException;
import com.cantor.journal.issue.Issue;
import com.cantor.journal.issue.IssueRepository;
import com.cantor.journal.notification.NotificationService;
import com.cantor.journal.notification.NotificationType;
import com.cantor.journal.request.PaperRequestRepository;
import com.cantor.journal.paper.dto.PaperDtos.DecisionRequest;
import com.cantor.journal.paper.dto.PaperDtos.PaperResponse;
import com.cantor.journal.review.ReviewAssignmentRepository;
import com.cantor.journal.review.ReviewRepository;
import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.Role;
import com.cantor.journal.user.User;
import com.cantor.journal.user.UserRepository;
import com.cantor.journal.user.dto.AuthDtos.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperRepository paperRepository;
    private final FileStorageService fileStorageService;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final CitationService citationService;
    private final ReferenceExtractionService referenceExtractionService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ManuscriptVersionRepository manuscriptVersionRepository;
    private final IssueRepository issueRepository;
    private final PaperRequestRepository paperRequestRepository;

    @Value("${app.review.double-blind:true}")
    private boolean doubleBlind;

    private String generateArticleCode(Paper paper) {
        int year = paper.getCreatedAt().atZone(java.time.ZoneOffset.UTC).getYear();
        return String.format("CJ-%d-%04d", year, paper.getId());
    }

    /** 편집자가 게재 확정 논문을 호에 배정하여 발행한다. */
    @Transactional
    public Paper publish(Long paperId, Long issueId, Integer pageStart, Integer pageEnd) {
        Paper paper = get(paperId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> ApiException.notFound("호를 찾을 수 없습니다."));
        paper.setIssue(issue);
        paper.setPageStart(pageStart);
        paper.setPageEnd(pageEnd);
        paper.setPublishedAt(java.time.Instant.now());
        paper.setStatus(PaperStatus.PUBLISHED);
        if (paper.getArticleCode() == null) {
            paper.setArticleCode(generateArticleCode(paper));
        }
        Paper saved = paperRepository.save(paper);
        notificationService.notify(
                saved.getSubmitter(),
                NotificationType.DECISION_MADE,
                "논문이 게재되었습니다: " + saved.getTitle() + " (" + issue.label() + ")",
                "/papers/" + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Paper> papersInIssue(Long issueId) {
        return paperRepository.findByIssueIdOrderByPageStartAsc(issueId);
    }

    @Transactional(readOnly = true)
    public List<Paper> list(PaperStatus status) {
        if (status != null) {
            return paperRepository.findByStatusOrderByUpdatedAtDesc(status);
        }
        return paperRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Paper> listMine(Long userId) {
        return paperRepository.findBySubmitterIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Paper> search(
            String q, String category, PaperStatus status, int page, int size) {
        String qn = (q == null || q.isBlank()) ? null : q.toLowerCase();
        String cat = (category == null || category.isBlank()) ? null : category;
        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"));
        return paperRepository.search(qn, cat, status, pageable);
    }

    @Transactional(readOnly = true)
    public Paper get(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("논문을 찾을 수 없습니다."));
    }

    /**
     * 열람 가능 여부: 게재된 논문은 누구나, 미게재 논문은 편집자/저자/배정 리뷰어만.
     */
    @Transactional(readOnly = true)
    public boolean canView(Paper p, UserPrincipal principal) {
        if (p.getStatus() == PaperStatus.PUBLISHED) {
            return true;
        }
        if (principal == null) {
            return false;
        }
        if (principal.getUser().getRoles().contains(Role.EDITOR)) {
            return true;
        }
        if (p.getSubmitter().getId().equals(principal.getId())) {
            return true;
        }
        return reviewAssignmentRepository.existsByPaperIdAndReviewerId(p.getId(), principal.getId());
    }

    /** 미게재 논문이면 열람 권한을 확인하고, 없으면 존재를 숨기기 위해 404를 던진다. */
    @Transactional(readOnly = true)
    public Paper getViewable(Long id, UserPrincipal principal) {
        Paper p = get(id);
        if (!canView(p, principal)) {
            throw ApiException.notFound("논문을 찾을 수 없습니다.");
        }
        return p;
    }

    /** 편집자가 논문 메타데이터(제목/저자/분야/키워드/초록)를 수정한다. */
    @Transactional
    public Paper editByEditor(Long id, String title, String authorsText, String category,
                              String articleType, String keywords, String abstractText) {
        Paper paper = get(id);
        if (title != null && !title.isBlank()) paper.setTitle(title);
        if (authorsText != null) paper.setAuthorsText(authorsText);
        if (category != null) paper.setCategory(category);
        if (articleType != null) paper.setArticleType(articleType);
        if (keywords != null) paper.setKeywords(keywords);
        if (abstractText != null) paper.setAbstractText(abstractText);
        return paperRepository.save(paper);
    }

    @Transactional
    public Paper submit(User submitter, String title, String abstractText,
                        String authorsText, String keywords, String category, String articleType,
                        MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("제목은 필수입니다.");
        }
        String stored = fileStorageService.store(file);
        Paper paper = Paper.builder()
                .title(title)
                .abstractText(abstractText)
                .authorsText(authorsText)
                .keywords(keywords)
                .category(category)
                .articleType(articleType)
                .status(PaperStatus.SUBMITTED)
                .submitter(submitter)
                .fileName(file.getOriginalFilename())
                .storedPath(stored)
                .fileSize(file.getSize())
                .build();
        paper = paperRepository.save(paper);
        // 영구 식별자 부여 (예: CJ-2026-0001)
        paper.setArticleCode(generateArticleCode(paper));
        paper = paperRepository.save(paper);
        // 참고문헌은 업로드된 PDF에서 자동 추출하여 인용 관계를 만든다.
        citationService.setReferences(paper,
                referenceExtractionService.extractCitedPaperIds(stored, paper.getId()));
        notificationService.notifyAll(
                userRepository.findByRolesContaining(Role.EDITOR),
                NotificationType.SUBMISSION_RECEIVED,
                "새 논문이 투고되었습니다: " + paper.getTitle(),
                "/papers/" + paper.getId());
        addVersion(paper, stored, file.getOriginalFilename(), file.getSize(), null);
        return paper;
    }

    /**
     * 저자가 수정본을 재제출한다(수정 라운드). 새 버전을 만들고 상태를 다시 심사중으로 되돌린다.
     */
    @Transactional
    public Paper resubmit(Long id, User actor, MultipartFile file, String responseToReviewers) {
        Paper paper = get(id);
        if (!paper.getSubmitter().getId().equals(actor.getId())) {
            throw ApiException.forbidden("본인이 제출한 논문만 재제출할 수 있습니다.");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("수정본 PDF를 첨부하세요.");
        }
        String stored = fileStorageService.store(file);
        paper.setStoredPath(stored);
        paper.setFileName(file.getOriginalFilename());
        paper.setFileSize(file.getSize());
        paper.setStatus(PaperStatus.UNDER_REVIEW);
        Paper saved = paperRepository.save(paper);

        citationService.setReferences(saved,
                referenceExtractionService.extractCitedPaperIds(stored, saved.getId()));
        addVersion(saved, stored, file.getOriginalFilename(), file.getSize(), responseToReviewers);
        notificationService.notifyAll(
                userRepository.findByRolesContaining(Role.EDITOR),
                NotificationType.RESUBMITTED,
                "수정본이 재제출되었습니다: " + saved.getTitle(),
                "/papers/" + saved.getId());
        return saved;
    }

    private void addVersion(Paper paper, String stored, String fileName, Long size, String response) {
        int next = manuscriptVersionRepository.findTopByPaperIdOrderByVersionNoDesc(paper.getId())
                .map(v -> v.getVersionNo() + 1).orElse(1);
        manuscriptVersionRepository.save(ManuscriptVersion.builder()
                .paper(paper)
                .versionNo(next)
                .fileName(fileName)
                .storedPath(stored)
                .fileSize(size)
                .responseToReviewers(response)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ManuscriptVersion> listVersions(Long paperId) {
        return manuscriptVersionRepository.findByPaperIdOrderByVersionNoAsc(paperId);
    }

    @Transactional(readOnly = true)
    public ManuscriptVersion getVersion(Long paperId, int versionNo) {
        return manuscriptVersionRepository.findByPaperIdAndVersionNo(paperId, versionNo)
                .orElseThrow(() -> ApiException.notFound("해당 버전을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Resource downloadVersion(Long paperId, int versionNo) {
        ManuscriptVersion v = getVersion(paperId, versionNo);
        return fileStorageService.loadAsResource(v.getStoredPath());
    }

    @Transactional
    public Paper update(Long id, User actor, String title, String abstractText,
                        String authorsText, String keywords, String category,
                        MultipartFile file) {
        Paper paper = get(id);
        if (!paper.getSubmitter().getId().equals(actor.getId())) {
            throw ApiException.forbidden("본인이 제출한 논문만 수정할 수 있습니다.");
        }
        if (title != null && !title.isBlank()) paper.setTitle(title);
        if (abstractText != null) paper.setAbstractText(abstractText);
        if (authorsText != null) paper.setAuthorsText(authorsText);
        if (keywords != null) paper.setKeywords(keywords);
        if (category != null) paper.setCategory(category);
        if (file != null && !file.isEmpty()) {
            String stored = fileStorageService.store(file);
            paper.setStoredPath(stored);
            paper.setFileName(file.getOriginalFilename());
            paper.setFileSize(file.getSize());
            // 새 PDF가 올라오면 참고문헌을 다시 추출한다.
            citationService.setReferences(paper,
                    referenceExtractionService.extractCitedPaperIds(stored, paper.getId()));
        }
        // 수정 요청 상태였다면 재제출로 간주
        if (paper.getStatus() == PaperStatus.REVISION_REQUESTED) {
            paper.setStatus(PaperStatus.UNDER_REVIEW);
        }
        return paperRepository.save(paper);
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) {
        Paper paper = get(id);
        if (paper.getStoredPath() == null) {
            throw ApiException.notFound("첨부된 파일이 없습니다.");
        }
        return fileStorageService.loadAsResource(paper.getStoredPath());
    }

    @Transactional
    public Paper decide(Long id, DecisionRequest req) {
        Paper paper = get(id);
        PaperStatus next = switch (req.decision() == null ? "" : req.decision().toUpperCase()) {
            case "ACCEPT", "ACCEPTED" -> PaperStatus.ACCEPTED;
            case "PUBLISH", "PUBLISHED" -> PaperStatus.PUBLISHED;
            case "REJECT", "REJECTED" -> PaperStatus.REJECTED;
            case "REVISION", "REVISION_REQUESTED" -> PaperStatus.REVISION_REQUESTED;
            default -> throw ApiException.badRequest("알 수 없는 결정 값입니다: " + req.decision());
        };
        paper.setStatus(next);
        paper.setDecisionNote(req.note());
        Paper saved = paperRepository.save(paper);

        NotificationType type = next == PaperStatus.REVISION_REQUESTED
                ? NotificationType.REVISION_REQUESTED : NotificationType.DECISION_MADE;
        notificationService.notify(
                saved.getSubmitter(),
                type,
                "논문 '" + saved.getTitle() + "' 에 대한 편집 결정: " + statusLabel(next),
                "/papers/" + saved.getId());
        return saved;
    }

    private String statusLabel(PaperStatus s) {
        return switch (s) {
            case ACCEPTED -> "게재 확정";
            case PUBLISHED -> "게재";
            case REJECTED -> "반려";
            case REVISION_REQUESTED -> "수정 요청";
            default -> s.name();
        };
    }

    /** 편집자가 논문을 삭제한다. 연결된 심사·배정 레코드와 업로드 파일도 함께 제거한다. */
    @Transactional
    public void delete(Long id) {
        Paper paper = get(id);
        reviewRepository.deleteByAssignmentPaperId(id);
        reviewAssignmentRepository.deleteByPaperId(id);
        citationService.deleteForPaper(id);
        paperRequestRepository.deleteByPaperId(id);
        manuscriptVersionRepository.findByPaperIdOrderByVersionNoAsc(id)
                .forEach(v -> fileStorageService.delete(v.getStoredPath()));
        manuscriptVersionRepository.deleteByPaperId(id);
        fileStorageService.delete(paper.getStoredPath());
        paperRepository.delete(paper);
    }

    /** 단일 논문 응답(피인용수 포함). 작성자/편집자 본인 호출용(맹검 없음). */
    @Transactional(readOnly = true)
    public PaperResponse response(Paper paper) {
        return PaperResponse.from(paper, citationService.citationCount(paper.getId()));
    }

    /** 요청자 기준으로 블라인드 테스트을 적용한 단일 응답. */
    @Transactional(readOnly = true)
    public PaperResponse response(Paper paper, UserPrincipal principal) {
        return blindIfNeeded(PaperResponse.from(paper, citationService.citationCount(paper.getId())),
                paper, principal);
    }

    /** 목록 응답(피인용수 배치 집계 + 블라인드 테스트). */
    @Transactional(readOnly = true)
    public List<PaperResponse> responses(List<Paper> papers, UserPrincipal principal) {
        List<Long> ids = papers.stream().map(Paper::getId).toList();
        Map<Long, Long> counts = citationService.citationCounts(ids);
        return papers.stream()
                .map(p -> blindIfNeeded(PaperResponse.from(p, counts.getOrDefault(p.getId(), 0L)), p, principal))
                .toList();
    }

    /** 블라인드 테스트: 리뷰어(편집자·저자 본인 제외)가 게재 전 논문을 볼 때 저자 정보를 가린다. */
    private PaperResponse blindIfNeeded(PaperResponse r, Paper p, UserPrincipal principal) {
        if (!doubleBlind || p.getStatus() == PaperStatus.PUBLISHED || principal == null) {
            return r;
        }
        Set<Role> roles = principal.getUser().getRoles();
        boolean editor = roles.contains(Role.EDITOR);
        boolean owner = p.getSubmitter().getId().equals(principal.getId());
        boolean reviewer = roles.contains(Role.REVIEWER);
        if (reviewer && !editor && !owner) {
            return new PaperResponse(
                    r.id(), r.title(), r.abstractText(),
                    "(블라인드 테스트 — 저자 비공개)", r.keywords(), r.category(), r.articleType(), r.status(),
                    new UserSummary(null, "익명", ""),
                    r.fileName(), r.fileSize(), r.decisionNote(), r.citationCount(),
                    r.articleCode(), r.issueLabel(), r.pages(), r.publishedAt(),
                    r.createdAt(), r.updatedAt());
        }
        return r;
    }

    @Transactional
    public void markUnderReview(Paper paper) {
        if (paper.getStatus() == PaperStatus.SUBMITTED) {
            paper.setStatus(PaperStatus.UNDER_REVIEW);
            paperRepository.save(paper);
        }
    }

    /** 모든 배정이 취소되었을 때 심사중 → 제출됨으로 되돌린다. */
    @Transactional
    public void revertToSubmitted(Long paperId) {
        Paper paper = get(paperId);
        if (paper.getStatus() == PaperStatus.UNDER_REVIEW) {
            paper.setStatus(PaperStatus.SUBMITTED);
            paperRepository.save(paper);
        }
    }
}
