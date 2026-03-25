package com.example.experienceplatform.campaign.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "regions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_region_sido_sigungu",
                columnNames = {"sido", "sigungu"}))
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String sido;

    @Column(nullable = false, length = 30)
    private String sigungu;

    @Column(name = "sido_short", length = 20)
    private String sidoShort;

    protected Region() {
    }

    public Region(String sido, String sigungu, String sidoShort) {
        this.sido = sido;
        this.sigungu = sigungu;
        this.sidoShort = sidoShort;
    }

    public Long getId() { return id; }
    public String getSido() { return sido; }
    public String getSigungu() { return sigungu; }
    public String getSidoShort() { return sidoShort; }
}
