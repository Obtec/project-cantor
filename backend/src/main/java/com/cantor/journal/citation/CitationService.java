package com.cantor.journal.citation;

import com.cantor.journal.citation.dto.CitationDtos.CitationGraph;
import com.cantor.journal.citation.dto.CitationDtos.Edge;
import com.cantor.journal.citation.dto.CitationDtos.PaperNode;
import com.cantor.journal.common.ApiException;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CitationService {

    /** 인용 그래프 탐색 최대 깊이(양방향). */
    private static final int MAX_DEPTH = 4;

    private final CitationRepository citationRepository;
    private final PaperRepository paperRepository;

    @Transactional(readOnly = true)
    public long citationCount(Long paperId) {
        return citationRepository.countByCitedPaperId(paperId);
    }

    /** 여러 논문의 피인용수를 한 번에 조회. */
    @Transactional(readOnly = true)
    public Map<Long, Long> citationCounts(Collection<Long> paperIds) {
        Map<Long, Long> result = new HashMap<>();
        if (paperIds == null || paperIds.isEmpty()) {
            return result;
        }
        for (Object[] row : citationRepository.countCitedGroupedByPaperIds(paperIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        // 인용이 0인 논문도 0으로 채운다.
        for (Long id : paperIds) {
            result.putIfAbsent(id, 0L);
        }
        return result;
    }

    /**
     * 논문의 참고문헌(인용 대상)을 주어진 목록으로 교체한다. (자기 인용/중복/미존재 제외)
     */
    @Transactional
    public void setReferences(Paper citingPaper, List<Long> citedPaperIds) {
        citationRepository.deleteByCitingPaperId(citingPaper.getId());
        if (citedPaperIds == null || citedPaperIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new LinkedHashSet<>(citedPaperIds);
        unique.remove(citingPaper.getId()); // 자기 인용 금지
        for (Long citedId : unique) {
            Paper cited = paperRepository.findById(citedId)
                    .orElseThrow(() -> ApiException.badRequest("존재하지 않는 인용 논문입니다: " + citedId));
            citationRepository.save(Citation.builder()
                    .citingPaper(citingPaper)
                    .citedPaper(cited)
                    .build());
        }
    }

    @Transactional
    public void deleteForPaper(Long paperId) {
        citationRepository.deleteByCitingPaperId(paperId);
        citationRepository.deleteByCitedPaperId(paperId);
    }

    /** 루트 논문을 기준으로 양방향 인용 그래프(트리)를 BFS로 구성한다. */
    @Transactional(readOnly = true)
    public CitationGraph graph(Long rootId) {
        Paper root = paperRepository.findById(rootId)
                .orElseThrow(() -> ApiException.notFound("논문을 찾을 수 없습니다."));

        Map<Long, Paper> nodePapers = new LinkedHashMap<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();

        nodePapers.put(root.getId(), root);

        Deque<Long> queue = new ArrayDeque<>();
        Map<Long, Integer> depth = new HashMap<>();
        queue.add(rootId);
        depth.put(rootId, 0);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            int d = depth.get(current);
            if (d >= MAX_DEPTH) {
                continue;
            }
            // outgoing: current 가 인용한 논문들
            for (Citation c : citationRepository.findByCitingPaperId(current)) {
                addEdge(edges, edgeKeys, current, c.getCitedPaper().getId());
                visit(c.getCitedPaper(), nodePapers, depth, queue, d + 1);
            }
            // incoming: current 를 인용한 논문들
            for (Citation c : citationRepository.findByCitedPaperId(current)) {
                addEdge(edges, edgeKeys, c.getCitingPaper().getId(), current);
                visit(c.getCitingPaper(), nodePapers, depth, queue, d + 1);
            }
        }

        Map<Long, Long> counts = citationCounts(nodePapers.keySet());
        List<PaperNode> nodes = nodePapers.values().stream()
                .map(p -> PaperNode.of(p, counts.getOrDefault(p.getId(), 0L), p.getId().equals(rootId)))
                .toList();

        return new CitationGraph(rootId, nodes, edges);
    }

    private void visit(Paper paper, Map<Long, Paper> nodePapers, Map<Long, Integer> depth,
                       Deque<Long> queue, int newDepth) {
        Long id = paper.getId();
        nodePapers.putIfAbsent(id, paper);
        if (!depth.containsKey(id)) {
            depth.put(id, newDepth);
            queue.add(id);
        }
    }

    private void addEdge(List<Edge> edges, Set<String> edgeKeys, Long from, Long to) {
        String key = from + "->" + to;
        if (edgeKeys.add(key)) {
            edges.add(new Edge(from, to));
        }
    }
}
