package com.cantor.journal.aireview;

import com.cantor.journal.aireview.dto.AiReviewDtos.AiReviewResponse;
import com.cantor.journal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 편집자 전용: AI 피어리뷰 생성 요청 및 조회.
 */
@RestController
@RequestMapping("/api/papers/{paperId}/ai-review")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EDITOR')")
public class AiReviewController {

    private final AiReviewService aiReviewService;

    @PostMapping
    public AiReviewResponse request(@PathVariable Long paperId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return AiReviewResponse.from(aiReviewService.request(paperId, principal.getUser()));
    }

    @GetMapping
    public List<AiReviewResponse> list(@PathVariable Long paperId) {
        return aiReviewService.listForPaper(paperId).stream()
                .map(AiReviewResponse::from)
                .toList();
    }
}
