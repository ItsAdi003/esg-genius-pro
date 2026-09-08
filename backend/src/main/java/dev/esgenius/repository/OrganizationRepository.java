package dev.esgenius.repository;

import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByTicker(String ticker);

    @Query("SELECT o FROM Organization o WHERE o.ticker IS NOT NULL ORDER BY o.name ASC")
    List<Organization> findAllListedCompanies();
}
