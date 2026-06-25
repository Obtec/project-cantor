package com.cantor.journal.metric.impl;

import com.cantor.journal.metric.AuthorMetric;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TotalCitationsMetric implements AuthorMetric {
    @Override public String key() { return "total_citations"; }
    @Override public String label() { return "총 피인용수"; }
    @Override public String description() { return "저자 논문들의 피인용수 합계"; }
    @Override public int order() { return 20; }

    @Override
    public double compute(List<Integer> citationCounts) {
        return citationCounts.stream().mapToInt(Integer::intValue).sum();
    }
}
