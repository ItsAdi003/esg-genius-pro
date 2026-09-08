package dev.esgenius.repository;

import dev.esgenius.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EsgRatingSnapshotRepositoryTest {

    @Autowired
    private EsgRatingSnapshotRepository ratingSnapshotRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void testFindLatestRatingSnapshotByOrganization() {
        // Setup
        Organization org = new Organization("Test Corp", "TCORP", "IT", "Tech", null);
        Organization saved = organizationRepository.save(org);

        EsgRatingSnapshot snapshot1 = new EsgRatingSnapshot(saved, 6.5, 6.0, 6.8, 6.9, EsgRatingBand.BBB,
                LocalDate.of(2026, 3, 31), 6.4);
        EsgRatingSnapshot snapshot2 = new EsgRatingSnapshot(saved, 7.0, 6.5, 7.2, 7.1, EsgRatingBand.A,
                LocalDate.of(2026, 6, 30), 6.5);

        ratingSnapshotRepository.save(snapshot1);
        ratingSnapshotRepository.save(snapshot2);

        // Test
        Optional<EsgRatingSnapshot> latest = ratingSnapshotRepository.findLatestByOrganization(saved);

        // Verify
        assertTrue(latest.isPresent());
        assertEquals(7.0, latest.get().getOverallScore());
        assertEquals(LocalDate.of(2026, 6, 30), latest.get().getAssessmentDate());
    }

    @Test
    void testFindRatingHistoryOrderedByDateDesc() {
        // Setup
        Organization org = new Organization("Test Corp", "TCORP", "IT", "Tech", null);
        Organization saved = organizationRepository.save(org);

        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(saved, 6.5, 6.0, 6.8, 6.9, EsgRatingBand.BBB, LocalDate.of(2026, 3, 31), 6.4));
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(saved, 7.0, 6.5, 7.2, 7.1, EsgRatingBand.A, LocalDate.of(2026, 6, 30), 6.5));
        ratingSnapshotRepository
                .save(new EsgRatingSnapshot(saved, 6.8, 6.3, 7.0, 7.0, EsgRatingBand.A, LocalDate.of(2026, 9, 7), 7.0));

        // Test
        List<EsgRatingSnapshot> history = ratingSnapshotRepository.findByOrganizationOrderByAssessmentDateDesc(saved);

        // Verify
        assertEquals(3, history.size());
        assertEquals(LocalDate.of(2026, 9, 7), history.get(0).getAssessmentDate());
        assertEquals(LocalDate.of(2026, 6, 30), history.get(1).getAssessmentDate());
        assertEquals(LocalDate.of(2026, 3, 31), history.get(2).getAssessmentDate());
    }
}
