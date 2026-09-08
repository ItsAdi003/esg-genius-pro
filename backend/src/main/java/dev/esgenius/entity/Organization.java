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

    @Column(length = 20)
    private String ticker;

    private String industry;

    private String sector;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Organization() {
    }

    public Organization(String name, String cin, String sector) {
        this.name = name;
        this.cin = cin;
        this.sector = sector;
        this.createdAt = Instant.now();
    }

    public Organization(String name, String ticker, String industry, String sector, String cin) {
        this.name = name;
        this.ticker = ticker;
        this.industry = industry;
        this.sector = sector;
        this.cin = cin;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
