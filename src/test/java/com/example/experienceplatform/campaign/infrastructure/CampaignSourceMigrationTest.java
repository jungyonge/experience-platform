package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CampaignSourceMigrationTest {

    @Autowired
    private CampaignSpringDataJpaRepository campaignRepository;

    @Autowired
    private CrawlingSourceSpringDataJpaRepository crawlingSourceRepository;

    private CrawlingSource revuSource;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        crawlingSourceRepository.deleteAll();

        revuSource = crawlingSourceRepository.save(
                new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1));
    }

    @Test
    @DisplayName("CrawlingSource 저장 후 Campaign 생성 - sourceCode/sourceName 매핑")
    void campaign_sourceCodeAndName() {
        Campaign campaign = new Campaign(
                revuSource, "1001", "테스트 캠페인", null, null,
                "https://revu.net/1001",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, LocalDate.of(2026, 3, 31), null);

        Campaign saved = campaignRepository.save(campaign);

        assertThat(saved.getSourceCode()).isEqualTo("REVU");
        assertThat(saved.getSourceName()).isEqualTo("레뷰");
    }

    @Test
    @DisplayName("findByCrawlingSourceAndOriginalId 동작 확인")
    void findByCrawlingSourceAndOriginalId() {
        Campaign campaign = new Campaign(
                revuSource, "2001", "검색 대상 캠페인", null, null,
                "https://revu.net/2001",
                CampaignCategory.BEAUTY, CampaignStatus.RECRUITING,
                3, null, null, null);
        campaignRepository.save(campaign);

        Optional<Campaign> found = campaignRepository.findByCrawlingSourceAndOriginalId(revuSource, "2001");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("검색 대상 캠페인");
        assertThat(found.get().getSourceCode()).isEqualTo("REVU");

        Optional<Campaign> notFound = campaignRepository.findByCrawlingSourceAndOriginalId(revuSource, "NONEXIST");
        assertThat(notFound).isEmpty();
    }
}
