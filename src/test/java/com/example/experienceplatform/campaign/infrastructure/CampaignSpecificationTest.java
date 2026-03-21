package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CampaignSpecificationTest {

    @Autowired
    private CampaignSpringDataJpaRepository repository;

    @Autowired
    private CrawlingSourceSpringDataJpaRepository crawlingSourceRepository;

    private CrawlingSource revuSource;
    private CrawlingSource gangnamSource;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        crawlingSourceRepository.deleteAll();

        revuSource = crawlingSourceRepository.save(
                new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1));
        gangnamSource = crawlingSourceRepository.save(
                new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 2));

        repository.save(new Campaign(revuSource, "T001", "맛집 체험단 모집", null, null,
                "https://revu.net/1", CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, LocalDate.of(2026, 3, 25), null));
        repository.save(new Campaign(gangnamSource, "T002", "뷰티 체험단", null, null,
                "https://gangnam.kr/2", CampaignCategory.BEAUTY, CampaignStatus.RECRUITING,
                10, null, LocalDate.of(2026, 3, 20), null));
        repository.save(new Campaign(gangnamSource, "T003", "강남 스테이크 하우스", null, null,
                "https://gangnam.kr/3", CampaignCategory.FOOD, CampaignStatus.CLOSED,
                3, null, LocalDate.of(2026, 3, 15), null));
        repository.save(new Campaign(revuSource, "T004", "여행 숙박 체험", null, null,
                "https://revu.net/4", CampaignCategory.TRAVEL, CampaignStatus.RECRUITING,
                2, null, LocalDate.of(2026, 4, 10), null));
        repository.save(new Campaign(gangnamSource, "T005", "디지털 기기 리뷰", null, null,
                "https://gangnam.kr/5", CampaignCategory.DIGITAL, CampaignStatus.CLOSED,
                7, null, LocalDate.of(2026, 3, 10), null));
    }

    @Test
    @DisplayName("조건 없으면 전체 조회")
    void noCondition_returnsAll() {
        CampaignSearchCondition condition = new CampaignSearchCondition(null, null, null, null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("키워드 검색 - 대소문자 무시")
    void keyword_caseInsensitive() {
        CampaignSearchCondition condition = new CampaignSearchCondition("맛집", null, null, null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).contains("맛집");
    }

    @Test
    @DisplayName("소스 필터 - IN 조건")
    void sourceTypeFilter() {
        CampaignSearchCondition condition = new CampaignSearchCondition(
                null, Set.of("REVU"), null, null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getSourceCode().equals("REVU"));
    }

    @Test
    @DisplayName("카테고리 필터 - IN 조건")
    void categoryFilter() {
        CampaignSearchCondition condition = new CampaignSearchCondition(
                null, null, Set.of(CampaignCategory.FOOD), null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getCategory() == CampaignCategory.FOOD);
    }

    @Test
    @DisplayName("상태 필터 - 동등 조건")
    void statusFilter() {
        CampaignSearchCondition condition = new CampaignSearchCondition(
                null, null, null, CampaignStatus.CLOSED);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getStatus() == CampaignStatus.CLOSED);
    }

    @Test
    @DisplayName("복합 조건 - 소스 + 카테고리 + 상태")
    void compositeFilter() {
        CampaignSearchCondition condition = new CampaignSearchCondition(
                null, Set.of("REVU"), Set.of(CampaignCategory.FOOD), CampaignStatus.RECRUITING);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).contains("맛집");
    }

    @Test
    @DisplayName("페이징 동작")
    void paging() {
        CampaignSearchCondition condition = new CampaignSearchCondition(null, null, null, null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> page1 = repository.findAll(spec, PageRequest.of(0, 2));
        Page<Campaign> page2 = repository.findAll(spec, PageRequest.of(1, 2));

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("정렬 - 마감일 ASC")
    void sorting_deadline() {
        CampaignSearchCondition condition = new CampaignSearchCondition(null, null, null, null);
        Specification<Campaign> spec = CampaignSpecification.withCondition(condition);

        Page<Campaign> result = repository.findAll(spec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "applyEndDate")));

        assertThat(result.getContent().get(0).getApplyEndDate())
                .isEqualTo(LocalDate.of(2026, 3, 10));
    }
}
