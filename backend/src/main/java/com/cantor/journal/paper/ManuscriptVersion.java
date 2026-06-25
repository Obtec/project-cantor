package com.cantor.journal.paper;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 원고 버전. 최초 투고가 v1, 수정 재제출마다 버전이 증가한다.
 */
@Entity
@Table(name = "manuscript_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"paper_id", "version_no"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManuscriptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    private String fileName;

    private String storedPath;

    private Long fileSize;

    /** 수정 재제출 시 저자가 작성하는 "리뷰어 의견에 대한 응답서". */
    @Column(columnDefinition = "TEXT")
    private String responseToReviewers;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
