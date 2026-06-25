package com.cantor.journal.paper;

import com.cantor.journal.issue.Issue;
import com.cantor.journal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "papers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(length = 1000)
    private String authorsText;

    @Column(length = 500)
    private String keywords;

    /** 분야(주제 영역). 다학제 저널이므로 수학 외 분야도 허용한다. */
    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaperStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id")
    private User submitter;

    private String fileName;

    private String storedPath;

    private Long fileSize;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(columnDefinition = "TEXT")
    private String decisionNote;

    /** 영구 논문 식별자(예: CJ-2026-0001). 최초 투고 시 부여. */
    @Column(unique = true)
    private String articleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    private Integer pageStart;

    private Integer pageEnd;

    private Instant publishedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PaperStatus.SUBMITTED;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
