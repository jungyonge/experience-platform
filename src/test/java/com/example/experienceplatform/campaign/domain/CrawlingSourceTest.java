package com.example.experienceplatform.campaign.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrawlingSourceTest {

    @Test
    @DisplayName("유효한 코드로 CrawlingSource 생성")
    void create_validCode() {
        CrawlingSource source = new CrawlingSource(
                "TEST_SRC", "테스트소스", "https://test.com",
                "/list?page={page}", "설명", "TEST", 1);

        assertThat(source.getCode()).isEqualTo("TEST_SRC");
        assertThat(source.getName()).isEqualTo("테스트소스");
        assertThat(source.getBaseUrl()).isEqualTo("https://test.com");
        assertThat(source.getListUrlPattern()).isEqualTo("/list?page={page}");
        assertThat(source.getDescription()).isEqualTo("설명");
        assertThat(source.getCrawlerType()).isEqualTo("TEST");
        assertThat(source.getDisplayOrder()).isEqualTo(1);
        assertThat(source.isActive()).isTrue();
    }

    @Test
    @DisplayName("null 코드로 생성 시 IllegalArgumentException")
    void create_nullCode() {
        assertThatThrownBy(() ->
                new CrawlingSource(null, "이름", "https://test.com", null, null, "TEST", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("너무 짧은 코드(1자)로 생성 시 IllegalArgumentException")
    void create_tooShortCode() {
        assertThatThrownBy(() ->
                new CrawlingSource("A", "이름", "https://test.com", null, null, "TEST", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("소문자 코드로 생성 시 IllegalArgumentException")
    void create_lowercaseCode() {
        assertThatThrownBy(() ->
                new CrawlingSource("revu", "이름", "https://test.com", null, null, "TEST", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("activate() - 비활성 → 활성")
    void activate() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        source.deactivate();
        assertThat(source.isActive()).isFalse();

        source.activate();
        assertThat(source.isActive()).isTrue();
    }

    @Test
    @DisplayName("deactivate() - 활성 → 비활성")
    void deactivate() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        assertThat(source.isActive()).isTrue();

        source.deactivate();
        assertThat(source.isActive()).isFalse();
    }

    @Test
    @DisplayName("update() 메서드로 필드 갱신")
    void update() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);

        source.update("레뷰v2", "https://revu-v2.net", "/list?p={page}",
                "업데이트된 설명", "REVU_V2", 10);

        assertThat(source.getName()).isEqualTo("레뷰v2");
        assertThat(source.getBaseUrl()).isEqualTo("https://revu-v2.net");
        assertThat(source.getListUrlPattern()).isEqualTo("/list?p={page}");
        assertThat(source.getDescription()).isEqualTo("업데이트된 설명");
        assertThat(source.getCrawlerType()).isEqualTo("REVU_V2");
        assertThat(source.getDisplayOrder()).isEqualTo(10);
    }

    @Test
    @DisplayName("update() 후에도 code는 변경되지 않음")
    void update_codeImmutable() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);

        source.update("새이름", "https://new.com", null, null, "NEW", 5);

        assertThat(source.getCode()).isEqualTo("REVU");
    }
}
