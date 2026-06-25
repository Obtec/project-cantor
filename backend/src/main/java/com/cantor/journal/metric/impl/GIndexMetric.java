package com.cantor.journal.metric.impl;

import com.cantor.journal.metric.AuthorMetric;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * g-index: 피인용수 내림차순 상위 g편의 누적 피인용수가 g² 이상인 최대 g.
 */
@Component
public class GIndexMetric implements AuthorMetric {
    @Override public String key() { return "g_index"; }
    @Override public String label() { return "g-index"; }
    @Override public String description() { return "상위 g편의 누적 피인용수가 g² 이상인 최대 g"; }
    @Override public int order() { return 40; }

    @Override
    public double compute(List<Integer> citationCounts) {
        List<Integer> sorted = citationCounts.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        int g = 0;
        long cumulative = 0;
        for (int i = 0; i < sorted.size(); i++) {
            cumulative += sorted.get(i);
            int candidate = i + 1;
            if (cumulative >= (long) candidate * candidate) {
                g = candidate;
            } else {
                break;
            }
        }
        return g;
    }
}
