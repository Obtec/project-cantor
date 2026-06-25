package com.cantor.journal.citation.dto;

import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperStatus;

import java.util.List;

public class CitationDtos {

    /** 그래프/목록에서 쓰는 논문 요약 노드. */
    public record PaperNode(
            Long id,
            String title,
            String authors,
            PaperStatus status,
            long citationCount,
            boolean root
    ) {
        public static PaperNode of(Paper p, long citationCount, boolean root) {
            return new PaperNode(
                    p.getId(),
                    p.getTitle(),
                    p.getAuthorsText() != null ? p.getAuthorsText()
                            : (p.getSubmitter() != null ? p.getSubmitter().getName() : ""),
                    p.getStatus(),
                    citationCount,
                    root
            );
        }
    }

    public record Edge(Long from, Long to) {}

    /** 루트 논문 주변의 인용 그래프(양방향, 다단계). */
    public record CitationGraph(
            Long root,
            List<PaperNode> nodes,
            List<Edge> edges
    ) {}
}
