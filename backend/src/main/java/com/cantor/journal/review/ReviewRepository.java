package com.cantor.journal.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByAssignmentId(Long assignmentId);

    List<Review> findByAssignmentPaperId(Long paperId);

    boolean existsByAssignmentId(Long assignmentId);

    void deleteByAssignmentPaperId(Long paperId);
}
