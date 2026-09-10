package dev.esgenius.repository;

import dev.esgenius.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EsgEventRepositoryTest {

    @Autowired
    private EsgEventRepository eventRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void testFindRecentEventsForOrganization() {
        // Setup
        Organization org = new Organization("Test Corp", "TCORP", "IT", "Tech", null);
        Organization saved = organizationRepository.save(org);

        eventRepository.save(new EsgEvent(saved, "Event 1", "Desc 1", EsgPillar.ENVIRONMENTAL, EsgEventSeverity.LOW,
                LocalDate.of(2026, 6, 1), 0.1, true));
        eventRepository.save(new EsgEvent(saved, "Event 2", "Desc 2", EsgPillar.SOCIAL, EsgEventSeverity.MEDIUM,
                LocalDate.of(2026, 7, 1), 0.2, true));
        eventRepository.save(new EsgEvent(saved, "Event 3", "Desc 3", EsgPillar.GOVERNANCE, EsgEventSeverity.HIGH,
                LocalDate.of(2026, 8, 1), -0.3, true));

        // Test
        List<EsgEvent> recent = eventRepository.findByOrganizationOrderByEventDateDesc(saved, PageRequest.of(0, 2));

        // Verify
        assertEquals(2, recent.size());
        assertEquals("Event 3", recent.get(0).getTitle());
        assertEquals("Event 2", recent.get(1).getTitle());
    }

    @Test
    void testFindPrototypeEvents() {
        // Setup
        Organization org = new Organization("Test Corp", "TCORP", "IT", "Tech", null);
        Organization saved = organizationRepository.save(org);

        eventRepository.save(new EsgEvent(saved, "Real Event", "Real", EsgPillar.ENVIRONMENTAL, EsgEventSeverity.LOW,
                LocalDate.of(2026, 6, 1), 0.1, false));
        eventRepository.save(new EsgEvent(saved, "Prototype Event", "Demo", EsgPillar.SOCIAL, EsgEventSeverity.MEDIUM,
                LocalDate.of(2026, 7, 1), 0.2, true));

        // Test
        List<EsgEvent> prototypeOnly = eventRepository.findByOrganizationAndIsPrototypeOrderByEventDateDesc(saved,
                true);

        // Verify
        assertEquals(1, prototypeOnly.size());
        assertEquals("Prototype Event", prototypeOnly.get(0).getTitle());
    }
}
