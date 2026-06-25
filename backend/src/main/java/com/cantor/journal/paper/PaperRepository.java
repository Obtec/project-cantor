package com.cantor.journal.paper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    @Query("""
            select p from Paper p
            where (:q is null
                   or lower(p.title) like concat('%', :q, '%')
                   or lower(coalesce(p.authorsText, '')) like concat('%', :q, '%')
                   or lower(coalesce(p.keywords, '')) like concat('%', :q, '%')
                   or lower(coalesce(p.abstractText, '')) like concat('%', :q, '%')
                   or lower(coalesce(p.articleCode, '')) like concat('%', :q, '%'))
              and (:category is null or p.category = :category)
              and (:status is null or p.status = :status)
            """)
    Page<Paper> search(@Param("q") String q,
                       @Param("category") String category,
                       @Param("status") PaperStatus status,
                       Pageable pageable);
    List<Paper> findByStatusOrderByUpdatedAtDesc(PaperStatus status);

    List<Paper> findBySubmitterIdOrderByUpdatedAtDesc(Long submitterId);

    List<Paper> findAllByOrderByUpdatedAtDesc();

    List<Paper> findByIssueIdOrderByPageStartAsc(Long issueId);

    int countByIssueId(Long issueId);
}
