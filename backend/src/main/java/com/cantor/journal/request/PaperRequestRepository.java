package com.cantor.journal.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRequestRepository extends JpaRepository<PaperRequest, Long> {

    List<PaperRequest> findByStatusOrderByCreatedAtDesc(PaperRequest.Status status);

    List<PaperRequest> findAllByOrderByCreatedAtDesc();

    List<PaperRequest> findByPaperIdOrderByCreatedAtDesc(Long paperId);

    void deleteByPaperId(Long paperId);
}
