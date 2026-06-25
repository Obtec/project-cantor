package com.cantor.journal.metric;

import com.cantor.journal.metric.dto.MetricDtos.MetricValue;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 등록된 모든 {@link AuthorMetric} 빈을 수집해 일괄 계산한다.
 * 지표 추가/삭제는 {@link AuthorMetric} 구현 빈을 추가/제거하는 것만으로 반영된다.
 */
@Service
public class MetricService {

    private final List<AuthorMetric> metrics;

    public MetricService(List<AuthorMetric> metrics) {
        this.metrics = metrics.stream()
                .sorted(Comparator.comparingInt(AuthorMetric::order))
                .toList();
    }

    public List<MetricValue> computeAll(List<Integer> citationCounts) {
        return metrics.stream()
                .map(m -> new MetricValue(
                        m.key(),
                        m.label(),
                        m.description(),
                        m.compute(citationCounts),
                        m.integerValued()))
                .toList();
    }
}
