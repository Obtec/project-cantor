package com.cantor.journal.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManuscriptVersionRepository extends JpaRepository<ManuscriptVersion, Long> {

    List<ManuscriptVersion> findByPaperIdOrderByVersionNoAsc(Long paperId);

    Optional<ManuscriptVersion> findByPaperIdAndVersionNo(Long paperId, int versionNo);

    Optional<ManuscriptVersion> findTopByPaperIdOrderByVersionNoDesc(Long paperId);

    void deleteByPaperId(Long paperId);
}
