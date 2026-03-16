package com.example.experienceplatform.campaign.infrastructure.crawling;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlingDateParser {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yy/MM/dd")
    );

    private static final Pattern D_DAY_PATTERN = Pattern.compile("D-?(\\d+)");
    private static final Pattern DAYS_BEFORE_PATTERN = Pattern.compile("(\\d+)일\\s*전");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");

    private CrawlingDateParser() {
    }

    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();

        // 표준 날짜 포맷
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (Exception ignored) {
            }
        }

        // D-day 패턴
        Matcher dDayMatcher = D_DAY_PATTERN.matcher(trimmed);
        if (dDayMatcher.find()) {
            int days = Integer.parseInt(dDayMatcher.group(1));
            return LocalDate.now().plusDays(days);
        }

        // N일전 패턴
        Matcher daysBefore = DAYS_BEFORE_PATTERN.matcher(trimmed);
        if (daysBefore.find()) {
            int days = Integer.parseInt(daysBefore.group(1));
            return LocalDate.now().plusDays(days);
        }

        // M월 D일 패턴
        Matcher monthDay = MONTH_DAY_PATTERN.matcher(trimmed);
        if (monthDay.find()) {
            int month = Integer.parseInt(monthDay.group(1));
            int day = Integer.parseInt(monthDay.group(2));
            return LocalDate.of(LocalDate.now().getYear(), month, day);
        }

        return null;
    }
}
