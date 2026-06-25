package com.cantor.journal.review;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.notification.NotificationService;
import com.cantor.journal.notification.NotificationType;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperService;
import com.cantor.journal.review.dto.ReviewDtos.AssignRequest;
import com.cantor.journal.review.dto.ReviewDtos.SubmitReviewRequest;
import com.cantor.journal.user.Role;
import com.cantor.journal.user.User;
import com.cantor.journal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewAssignmentRepository assignmentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PaperService paperService;
    private final NotificationService notificationService;

    @Transactional
    public ReviewAssignment assign(Long paperId, AssignRequest req, User editor) {
        Paper paper = paperService.get(paperId);
        User reviewer = userRepository.findById(req.reviewerId())
                .orElseThrow(() -> ApiException.notFound("리뷰어를 찾을 수 없습니다."));
        if (assignmentRepository.existsByPaperIdAndReviewerId(paperId, reviewer.getId())) {
            throw ApiException.conflict("이미 해당 리뷰어에게 배정된 논문입니다.");
        }
        // 리뷰어 역할 부여(아직 없다면)
        if (!reviewer.getRoles().contains(Role.REVIEWER)) {
            reviewer.getRoles().add(Role.REVIEWER);
            userRepository.save(reviewer);
        }
        ReviewAssignment assignment = ReviewAssignment.builder()
                .paper(paper)
                .reviewer(reviewer)
                .assignedBy(editor)
                .status(AssignmentStatus.PENDING)
                .dueDate(req.dueDate())
                .build();
        assignment = assignmentRepository.save(assignment);
        paperService.markUnderReview(paper);
        notificationService.notify(
                reviewer,
                NotificationType.REVIEWER_ASSIGNED,
                "심사가 배정되었습니다: " + paper.getTitle(),
                "/reviews");
        return assignment;
    }

    /** 편집자가 리뷰어 배정을 취소한다. 제출된 심사가 있으면 함께 삭제하고 리뷰어에게 알린다. */
    @Transactional
    public void cancelAssignment(Long paperId, Long assignmentId) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("배정을 찾을 수 없습니다."));
        if (!assignment.getPaper().getId().equals(paperId)) {
            throw ApiException.badRequest("해당 논문의 배정이 아닙니다.");
        }
        boolean lastOne = assignmentRepository.findByPaperId(paperId).size() <= 1;
        User reviewer = assignment.getReviewer();
        String title = assignment.getPaper().getTitle();

        reviewRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.delete(assignment);

        if (lastOne) {
            paperService.revertToSubmitted(paperId);
        }
        notificationService.notify(
                reviewer,
                NotificationType.ASSIGNMENT_CANCELLED,
                "심사 배정이 취소되었습니다: " + title,
                "/reviews");
    }

    @Transactional(readOnly = true)
    public List<ReviewAssignment> assignmentsForReviewer(Long reviewerId) {
        return assignmentRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId);
    }

    @Transactional(readOnly = true)
    public List<ReviewAssignment> assignmentsForPaper(Long paperId) {
        return assignmentRepository.findByPaperId(paperId);
    }

    @Transactional(readOnly = true)
    public boolean reviewSubmitted(Long assignmentId) {
        return reviewRepository.existsByAssignmentId(assignmentId);
    }

    @Transactional(readOnly = true)
    public List<Review> reviewsForPaper(Long paperId) {
        return reviewRepository.findByAssignmentPaperId(paperId);
    }

    @Transactional
    public Review submitReview(User reviewer, SubmitReviewRequest req) {
        ReviewAssignment assignment = assignmentRepository
                .findByIdAndReviewerId(req.assignmentId(), reviewer.getId())
                .orElseThrow(() -> ApiException.forbidden("본인에게 배정된 심사 건이 아닙니다."));
        if (reviewRepository.existsByAssignmentId(assignment.getId())) {
            throw ApiException.conflict("이미 심사를 제출했습니다.");
        }
        if (req.score() != null && (req.score() < 1 || req.score() > 5)) {
            throw ApiException.badRequest("점수는 1~5 사이여야 합니다.");
        }
        Review review = Review.builder()
                .assignment(assignment)
                .recommendation(req.recommendation())
                .commentsToEditor(req.commentsToEditor())
                .commentsToAuthor(req.commentsToAuthor())
                .score(req.score())
                .build();
        review = reviewRepository.save(review);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(assignment);
        notificationService.notifyAll(
                userRepository.findByRolesContaining(Role.EDITOR),
                NotificationType.REVIEW_SUBMITTED,
                "심사가 제출되었습니다: " + assignment.getPaper().getTitle(),
                "/papers/" + assignment.getPaper().getId());
        return review;
    }
}
