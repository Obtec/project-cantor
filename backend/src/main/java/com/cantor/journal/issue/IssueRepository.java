package com.cantor.journal.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findAllByOrderByYearDescVolumeDescNumberDesc();

    boolean existsByVolumeAndNumber(int volume, int number);
}
