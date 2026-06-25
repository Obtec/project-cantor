package com.cantor.journal.issue;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 저널의 권(volume)/호(number). 게재 확정된 논문이 호에 배정되어 발행된다.
 */
@Entity
@Table(name = "issues",
        uniqueConstraints = @UniqueConstraint(columnNames = {"volume", "number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int volume;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private int year;

    private String title;

    @Column(nullable = false)
    private boolean published;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public String label() {
        return "Vol. " + volume + ", No. " + number + " (" + year + ")";
    }
}
