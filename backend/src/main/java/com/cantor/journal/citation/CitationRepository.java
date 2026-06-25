package com.cantor.journal.citation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CitationRepository extends JpaRepository<Citation, Long> {

    /** 이 논문이 인용한 논문들(참고문헌, outgoing). */
    List<Citation> findByCitingPaperId(Long citingPaperId);

    /** 이 논문을 인용한 논문들(피인용, incoming). */
    List<Citation> findByCitedPaperId(Long citedPaperId);

    long countByCitedPaperId(Long citedPaperId);

    boolean existsByCitingPaperIdAndCitedPaperId(Long citingPaperId, Long citedPaperId);

    void deleteByCitingPaperId(Long citingPaperId);

    void deleteByCitedPaperId(Long citedPaperId);

    /** 여러 논문의 피인용수를 한 번에 집계: [citedPaperId, count]. */
    @Query("select c.citedPaper.id, count(c) from Citation c where c.citedPaper.id in :ids group by c.citedPaper.id")
    List<Object[]> countCitedGroupedByPaperIds(@Param("ids") Collection<Long> ids);
}
