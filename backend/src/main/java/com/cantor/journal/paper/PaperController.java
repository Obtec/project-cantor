package com.cantor.journal.paper;

import com.cantor.journal.citation.CitationService;
import com.cantor.journal.citation.dto.CitationDtos.CitationGraph;
import com.cantor.journal.common.ApiException;
import com.cantor.journal.issue.dto.IssueDtos.PublishPaperRequest;
import com.cantor.journal.paper.dto.PaperDtos.DecisionRequest;
import com.cantor.journal.paper.dto.PaperDtos.EditMetadataRequest;
import com.cantor.journal.paper.dto.PaperDtos.PaperResponse;
import com.cantor.journal.paper.dto.VersionDtos.VersionResponse;
import com.cantor.journal.review.Review;
import com.cantor.journal.review.ReviewService;
import com.cantor.journal.review.dto.ReviewDtos.ReviewResponse;
import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final CitationService citationService;
    private final ReviewService reviewService;

    @GetMapping
    public List<PaperResponse> list(@RequestParam(required = false) PaperStatus status,
                                    @RequestParam(required = false, defaultValue = "false") boolean mine,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        List<Paper> papers;
        if (mine && principal != null) {
            papers = paperService.listMine(principal.getId());
        } else if (isEditor(principal)) {
            papers = paperService.list(status);
        } else {
            // 비편집자: 게재된 논문만 노출
            papers = paperService.list(PaperStatus.PUBLISHED);
        }
        return paperService.responses(papers, principal);
    }

    @GetMapping("/{id}")
    public PaperResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return paperService.response(paperService.getViewable(id, principal), principal);
    }

    /** 서버 검색 + 페이지네이션. 비편집자에게는 게재된 논문만 노출. */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) PaperStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        PaperStatus effectiveStatus = isEditor(principal) ? status : PaperStatus.PUBLISHED;
        var result = paperService.search(q, category, effectiveStatus, page, size);
        return Map.of(
                "content", paperService.responses(result.getContent(), principal),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages());
    }

    /** RIS(EndNote/Zotero 등) 형식 인용 내보내기. */
    @GetMapping(value = "/{id}/cite.ris", produces = "application/x-research-info-systems")
    public ResponseEntity<String> ris(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        Paper p = paperService.getViewable(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + (p.getArticleCode() == null ? "paper" : p.getArticleCode()) + ".ris\"")
                .body(buildRis(p));
    }

    private String buildRis(Paper p) {
        int year = (p.getPublishedAt() != null ? p.getPublishedAt() : p.getCreatedAt())
                .atZone(ZoneOffset.UTC).getYear();
        StringBuilder sb = new StringBuilder();
        sb.append("TY  - JOUR\n");
        sb.append("TI  - ").append(nv(p.getTitle())).append('\n');
        for (String author : authors(p)) {
            sb.append("AU  - ").append(author).append('\n');
        }
        sb.append("PY  - ").append(year).append('\n');
        sb.append("JO  - Cantor Journal\n");
        if (p.getIssue() != null) {
            sb.append("VL  - ").append(p.getIssue().getVolume()).append('\n');
            sb.append("IS  - ").append(p.getIssue().getNumber()).append('\n');
        }
        if (p.getPageStart() != null) sb.append("SP  - ").append(p.getPageStart()).append('\n');
        if (p.getPageEnd() != null) sb.append("EP  - ").append(p.getPageEnd()).append('\n');
        if (p.getKeywords() != null) {
            for (String kw : p.getKeywords().split("[,;]")) {
                if (!kw.isBlank()) sb.append("KW  - ").append(kw.trim()).append('\n');
            }
        }
        if (p.getAbstractText() != null) sb.append("AB  - ").append(nv(p.getAbstractText())).append('\n');
        if (p.getArticleCode() != null) sb.append("SN  - ").append(p.getArticleCode()).append('\n');
        sb.append("ER  - \n");
        return sb.toString();
    }

    private List<String> authors(Paper p) {
        String src = p.getAuthorsText() != null && !p.getAuthorsText().isBlank()
                ? p.getAuthorsText()
                : (p.getSubmitter() != null ? p.getSubmitter().getName() : "");
        return Arrays.stream(src.split("[,;]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String nv(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    @GetMapping("/{id}/citation-graph")
    public CitationGraph citationGraph(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        paperService.getViewable(id, principal);
        return citationService.graph(id);
    }

    private boolean isEditor(UserPrincipal principal) {
        return principal != null && principal.getUser().getRoles().contains(Role.EDITOR);
    }

    /** 심사 의견 조회: 편집자는 전체, 저자(제출자)는 결정 후 익명 의견만 본다. */
    @GetMapping("/{id}/reviews")
    @PreAuthorize("isAuthenticated()")
    public List<ReviewResponse> reviews(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        Paper paper = paperService.get(id);
        boolean editor = principal.getUser().getRoles().contains(Role.EDITOR);
        boolean owner = paper.getSubmitter().getId().equals(principal.getId());
        if (!editor && !owner) {
            throw ApiException.forbidden("심사 의견을 볼 권한이 없습니다.");
        }
        List<Review> reviews = reviewService.reviewsForPaper(id);
        if (editor) {
            return reviews.stream().map(ReviewResponse::from).toList();
        }
        // 저자: 편집 결정이 내려진 뒤에만 익명 의견 공개
        boolean decided = switch (paper.getStatus()) {
            case REVISION_REQUESTED, ACCEPTED, REJECTED, PUBLISHED -> true;
            default -> false;
        };
        if (!decided) {
            return List.of();
        }
        List<ReviewResponse> out = new java.util.ArrayList<>();
        int index = 1;
        for (Review r : reviews) {
            out.add(ReviewResponse.authorView(r, index++));
        }
        return out;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PaperResponse submit(@RequestParam String title,
                                @RequestParam(required = false) String abstractText,
                                @RequestParam(required = false) String authorsText,
                                @RequestParam(required = false) String keywords,
                                @RequestParam(required = false) String category,
                                @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return paperService.response(
                paperService.submit(principal.getUser(), title, abstractText, authorsText, keywords, category, file));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PaperResponse update(@PathVariable Long id,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String abstractText,
                                @RequestParam(required = false) String authorsText,
                                @RequestParam(required = false) String keywords,
                                @RequestParam(required = false) String category,
                                @RequestPart(value = "file", required = false) MultipartFile file,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return paperService.response(
                paperService.update(id, principal.getUser(), title, abstractText, authorsText, keywords, category, file));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Paper paper = paperService.getViewable(id, principal);
        Resource resource = paperService.download(id);
        String filename = paper.getFileName() == null ? "paper.pdf" : paper.getFileName();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('EDITOR')")
    public PaperResponse decide(@PathVariable Long id, @RequestBody DecisionRequest req) {
        return paperService.response(paperService.decide(id, req));
    }

    /** 편집자가 논문 메타데이터(제목/저자/분야/키워드/초록)를 수정한다. */
    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasRole('EDITOR')")
    public PaperResponse editMetadata(@PathVariable Long id, @RequestBody EditMetadataRequest req) {
        return paperService.response(paperService.editByEditor(
                id, req.title(), req.authorsText(), req.category(), req.keywords(), req.abstractText()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 편집자가 논문을 호에 배정하여 게재 발행한다. */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('EDITOR')")
    public PaperResponse publish(@PathVariable Long id, @RequestBody PublishPaperRequest req) {
        return paperService.response(
                paperService.publish(id, req.issueId(), req.pageStart(), req.pageEnd()));
    }

    /** 저자의 수정본 재제출(새 버전 생성 + 리뷰어 응답서). */
    @PostMapping(path = "/{id}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PaperResponse resubmit(@PathVariable Long id,
                                  @RequestPart("file") MultipartFile file,
                                  @RequestParam(required = false) String responseToReviewers,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return paperService.response(
                paperService.resubmit(id, principal.getUser(), file, responseToReviewers), principal);
    }

    /** 원고 버전 이력(편집자 또는 저자 본인). */
    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public List<VersionResponse> versions(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        requireEditorOrOwner(id, principal);
        return paperService.listVersions(id).stream().map(VersionResponse::from).toList();
    }

    @GetMapping("/{id}/versions/{versionNo}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadVersion(@PathVariable Long id, @PathVariable int versionNo,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        requireEditorOrOwner(id, principal);
        ManuscriptVersion v = paperService.getVersion(id, versionNo);
        Resource resource = paperService.downloadVersion(id, versionNo);
        String filename = v.getFileName() == null ? "paper.pdf" : v.getFileName();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    private void requireEditorOrOwner(Long id, UserPrincipal principal) {
        Paper paper = paperService.get(id);
        boolean editor = principal.getUser().getRoles().contains(Role.EDITOR);
        boolean owner = paper.getSubmitter().getId().equals(principal.getId());
        if (!editor && !owner) {
            throw ApiException.forbidden("권한이 없습니다.");
        }
    }
}
