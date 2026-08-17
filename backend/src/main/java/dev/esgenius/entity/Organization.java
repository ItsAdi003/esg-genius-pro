package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String cin;

    private String sector;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Organization() {}

    public Organization(String name, String cin, String sector) {
        this.name = name;
        this.cin = cin;
        this.sector = sector;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public Instant getCreatedAt() { return createdAt; }
}
