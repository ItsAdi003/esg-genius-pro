package dev.esgenius.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "framework")
public class Framework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String code;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 100)
    private String region;

    @Column(length = 50)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FrameworkStatus status = FrameworkStatus.ACTIVE;

    @OneToMany(mappedBy = "framework", fetch = FetchType.LAZY)
    private List<FrameworkRequirement> requirements = new ArrayList<>();

    protected Framework() {}

    public Framework(String code, String name, String fullName, String region, String version, FrameworkStatus status) {
        this.code = code;
        this.name = name;
        this.fullName = fullName;
        this.region = region;
        this.version = version;
        this.status = status;
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public FrameworkStatus getStatus() { return status; }
    public void setStatus(FrameworkStatus status) { this.status = status; }

    public List<FrameworkRequirement> getRequirements() { return requirements; }
}
