package com.cantor.journal.metric.impl;

import com.cantor.journal.metric.AuthorMetric;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * h-index: 피인용수가 h 이상인 논문이 h편 이상 존재하는 최대 h.
 */
@Component
public class HIndexMetric implements AuthorMetric {
    @Override public String key() { return "h_index"; }
    @Override public String label() { return "h-index"; }
    @Override public String description() { return "피인용수가 h 이상인 논문이 h편 이상인 최대 h"; }
    @Override public int order() { return 30; }

    @Override
    public double compute(List<Integer> citationCounts) {
        List<Integer> sorted = citationCounts.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        int h = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) >= i + 1) {
                h = i + 1;
            } else {
                break;
            }
        }
        return h;
    }
}
