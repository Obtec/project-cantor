package com.cantor.journal.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    List<ReviewAssignment> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);

    List<ReviewAssignment> findByPaperId(Long paperId);

    boolean existsByPaperIdAndReviewerId(Long paperId, Long reviewerId);

    Optional<ReviewAssignment> findByIdAndReviewerId(Long id, Long reviewerId);

    void deleteByPaperId(Long paperId);
}
