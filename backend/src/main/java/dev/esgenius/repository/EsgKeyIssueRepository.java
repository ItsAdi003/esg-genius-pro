package dev.esgenius.repository;

import dev.esgenius.entity.EsgKeyIssue;
import dev.esgenius.entity.EsgPillar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EsgKeyIssueRepository extends JpaRepository<EsgKeyIssue, Long> {

    Optional<EsgKeyIssue> findByCode(String code);

    List<EsgKeyIssue> findByPillar(EsgPillar pillar);

    List<EsgKeyIssue> findAllByOrderByPillarAscNameAsc();
}
