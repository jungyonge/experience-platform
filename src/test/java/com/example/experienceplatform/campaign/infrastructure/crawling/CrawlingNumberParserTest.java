package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlingNumberParserTest {

    @Test
    @DisplayName("5명 모집 → 5")
    void parse_withText() {
        assertThat(CrawlingNumberParser.parse("5명 모집")).isEqualTo(5);
    }

    @Test
    @DisplayName("인원 10 → 10")
    void parse_leading() {
        assertThat(CrawlingNumberParser.parse("인원 10")).isEqualTo(10);
    }

    @Test
    @DisplayName("숫자만 → 정상")
    void parse_numberOnly() {
        assertThat(CrawlingNumberParser.parse("15")).isEqualTo(15);
    }

    @Test
    @DisplayName("null → null")
    void parse_null() {
        assertThat(CrawlingNumberParser.parse(null)).isNull();
    }

    @Test
    @DisplayName("숫자 없음 → null")
    void parse_noNumber() {
        assertThat(CrawlingNumberParser.parse("없음")).isNull();
    }
}
