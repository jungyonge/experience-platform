package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlingDateParserTest {

    @Test
    @DisplayName("yyyy.MM.dd 형식 파싱")
    void parse_dotFormat() {
        assertThat(CrawlingDateParser.parse("2026.03.25")).isEqualTo(LocalDate.of(2026, 3, 25));
    }

    @Test
    @DisplayName("yyyy-MM-dd 형식 파싱")
    void parse_dashFormat() {
        assertThat(CrawlingDateParser.parse("2026-03-25")).isEqualTo(LocalDate.of(2026, 3, 25));
    }

    @Test
    @DisplayName("M월 D일 형식 파싱")
    void parse_monthDay() {
        LocalDate result = CrawlingDateParser.parse("3월 25일");
        assertThat(result).isNotNull();
        assertThat(result.getMonthValue()).isEqualTo(3);
        assertThat(result.getDayOfMonth()).isEqualTo(25);
    }

    @Test
    @DisplayName("null → null")
    void parse_null() {
        assertThat(CrawlingDateParser.parse(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열 → null")
    void parse_blank() {
        assertThat(CrawlingDateParser.parse("")).isNull();
    }

    @Test
    @DisplayName("파싱 불가 → null")
    void parse_invalid() {
        assertThat(CrawlingDateParser.parse("알수없는날짜")).isNull();
    }
}
