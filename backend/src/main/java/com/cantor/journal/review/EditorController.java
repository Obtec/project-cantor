package com.cantor.journal.review;

import com.cantor.journal.review.dto.ReviewDtos.AssignRequest;
import com.cantor.journal.review.dto.ReviewDtos.AssignmentResponse;
import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.UserRepository;
import com.cantor.journal.user.dto.AuthDtos.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 편집자 전용 엔드포인트: 리뷰어 후보 조회, 배정, 심사 현황 확인.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EDITOR')")
public class EditorController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/editor/reviewers")
    public List<UserSummary> reviewers() {
        return userRepository.findAll().stream()
                .map(UserSummary::from)
                .toList();
    }

    @PostMapping("/papers/{paperId}/assign")
    public AssignmentResponse assign(@PathVariable Long paperId,
                                     @Valid @RequestBody AssignRequest req,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        ReviewAssignment a = reviewService.assign(paperId, req, principal.getUser());
        return AssignmentResponse.from(a, false);
    }

    @GetMapping("/papers/{paperId}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long paperId) {
        return reviewService.assignmentsForPaper(paperId).stream()
                .map(a -> AssignmentResponse.from(a, reviewService.reviewSubmitted(a.getId())))
                .toList();
    }

    @DeleteMapping("/papers/{paperId}/assignments/{assignmentId}")
    public ResponseEntity<Void> cancelAssignment(@PathVariable Long paperId,
                                                 @PathVariable Long assignmentId) {
        reviewService.cancelAssignment(paperId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
