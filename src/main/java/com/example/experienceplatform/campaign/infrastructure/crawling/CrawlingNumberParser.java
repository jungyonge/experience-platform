package com.example.experienceplatform.campaign.infrastructure.crawling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlingNumberParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private CrawlingNumberParser() {
    }

    public static Integer parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
