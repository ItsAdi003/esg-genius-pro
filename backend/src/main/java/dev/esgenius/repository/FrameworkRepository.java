package dev.esgenius.repository;

import dev.esgenius.entity.Framework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrameworkRepository extends JpaRepository<Framework, Long> {

    Optional<Framework> findByCode(String code);
}
