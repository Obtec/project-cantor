package com.cantor.journal.metric.impl;

import com.cantor.journal.metric.AuthorMetric;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaperCountMetric implements AuthorMetric {
    @Override public String key() { return "paper_count"; }
    @Override public String label() { return "논문 수"; }
    @Override public String description() { return "저자가 제출한 논문 수"; }
    @Override public int order() { return 10; }

    @Override
    public double compute(List<Integer> citationCounts) {
        return citationCounts.size();
    }
}
