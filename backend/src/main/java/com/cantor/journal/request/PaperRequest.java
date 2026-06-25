package com.cantor.journal.request;

import com.cantor.journal.paper.Paper;
import com.cantor.journal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 저자가 본인 논문에 대해 편집장에게 보내는 수정/삭제 요청.
 */
@Entity
@Table(name = "paper_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperRequest {

    public enum Type { EDIT, DELETE }

    public enum Status { PENDING, RESOLVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String editorNote;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = Status.PENDING;
        }
    }
}
