package com.example.experienceplatform.campaign.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "crawling_sources",
        uniqueConstraints = @UniqueConstraint(name = "uq_crawling_source_code", columnNames = "code"))
public class CrawlingSource {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_]{2,30}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 300)
    private String baseUrl;

    @Column(length = 500)
    private String listUrlPattern;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String crawlerType;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected CrawlingSource() {
    }

    public CrawlingSource(String code, String name, String baseUrl, String listUrlPattern,
                          String description, String crawlerType, int displayOrder) {
        validateCode(code);
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
        this.listUrlPattern = listUrlPattern;
        this.description = description;
        this.crawlerType = crawlerType;
        this.active = true;
        this.displayOrder = displayOrder;
    }

    private void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "코드는 영문 대문자, 숫자, 언더스코어만 허용되며 2~30자여야 합니다: " + code);
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String name, String baseUrl, String listUrlPattern,
                       String description, String crawlerType, int displayOrder) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.listUrlPattern = listUrlPattern;
        this.description = description;
        this.crawlerType = crawlerType;
        this.displayOrder = displayOrder;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getListUrlPattern() { return listUrlPattern; }
    public String getDescription() { return description; }
    public String getCrawlerType() { return crawlerType; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
