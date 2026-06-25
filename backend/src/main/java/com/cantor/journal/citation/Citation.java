package com.cantor.journal.citation;

import com.cantor.journal.paper.Paper;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 칸토어 저널 내 논문 간 인용 엣지: citingPaper 가 citedPaper 를 인용한다.
 */
@Entity
@Table(name = "citations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"citing_paper_id", "cited_paper_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citing_paper_id")
    private Paper citingPaper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cited_paper_id")
    private Paper citedPaper;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
