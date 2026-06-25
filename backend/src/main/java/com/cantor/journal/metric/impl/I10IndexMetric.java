package com.cantor.journal.metric.impl;

import com.cantor.journal.metric.AuthorMetric;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * i10-index: 피인용수가 10 이상인 논문의 수.
 */
@Component
public class I10IndexMetric implements AuthorMetric {
    @Override public String key() { return "i10_index"; }
    @Override public String label() { return "i10-index"; }
    @Override public String description() { return "피인용수가 10 이상인 논문 수"; }
    @Override public int order() { return 50; }

    @Override
    public double compute(List<Integer> citationCounts) {
        return citationCounts.stream().filter(c -> c >= 10).count();
    }
}
