package com.cantor.journal.aireview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AiReviewRepository extends JpaRepository<AiReview, Long> {

    List<AiReview> findByPaperIdOrderByCreatedAtDesc(Long paperId);

    boolean existsByPaperIdAndStatusAndCreatedAtAfter(Long paperId, AiReviewStatus status, Instant createdAfter);
}
