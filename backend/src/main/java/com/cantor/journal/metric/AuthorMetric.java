package com.cantor.journal.metric;

import java.util.List;

/**
 * 저자 단위 학술 지표 계산기.
 *
 * <p>새 지표를 추가하려면 이 인터페이스를 구현한 {@code @Component} 클래스를 하나 만들면 되고,
 * 지표를 없애려면 해당 클래스를 삭제(또는 빈 등록 해제)하면 된다. {@link MetricService} 가
 * 등록된 모든 구현체를 자동으로 수집한다.</p>
 */
public interface AuthorMetric {

    /** 식별 키(예: "h_index"). API/프론트에서 안정적으로 참조된다. */
    String key();

    /** 표시 이름(예: "h-index"). */
    String label();

    /** 설명(툴팁 등). */
    String description();

    /**
     * 저자가 보유한 각 논문의 피인용수 목록으로부터 지표 값을 계산한다.
     *
     * @param citationCounts 논문별 피인용수(정렬 보장 없음)
     */
    double compute(List<Integer> citationCounts);

    /** 정수 지표 여부(표시 포맷용). */
    default boolean integerValued() {
        return true;
    }

    /** 표시 순서(작을수록 먼저). */
    default int order() {
        return 100;
    }
}
