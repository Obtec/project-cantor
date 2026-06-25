package com.cantor.journal.review;

import com.cantor.journal.review.dto.ReviewDtos.AssignmentResponse;
import com.cantor.journal.review.dto.ReviewDtos.ReviewResponse;
import com.cantor.journal.review.dto.ReviewDtos.SubmitReviewRequest;
import com.cantor.journal.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/assigned")
    @PreAuthorize("isAuthenticated()")
    public List<AssignmentResponse> assigned(@AuthenticationPrincipal UserPrincipal principal) {
        return reviewService.assignmentsForReviewer(principal.getId()).stream()
                .map(a -> AssignmentResponse.from(a, reviewService.reviewSubmitted(a.getId())))
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse submit(@Valid @RequestBody SubmitReviewRequest req,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ReviewResponse.from(reviewService.submitReview(principal.getUser(), req));
    }
}
