package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RobotsTxtChecker {

    private static final Logger log = LoggerFactory.getLogger(RobotsTxtChecker.class);

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();
    private final CrawlingProperties properties;

    public RobotsTxtChecker(CrawlingProperties properties) {
        this.properties = properties;
    }

    public boolean isAllowed(String baseUrl, String path) {
        String cacheKey = baseUrl + "|" + path;
        return cache.computeIfAbsent(cacheKey, key -> checkRobotsTxt(baseUrl, path));
    }

    private boolean checkRobotsTxt(String baseUrl, String path) {
        try {
            String robotsUrl = baseUrl + "/robots.txt";
            String robotsContent = Jsoup.connect(robotsUrl)
                    .userAgent(properties.getUserAgent())
                    .timeout(5000)
                    .execute()
                    .body();

            return !isDisallowed(robotsContent, path);
        } catch (Exception e) {
            log.warn("robots.txt 조회 실패 ({}), 허용으로 처리합니다: {}", baseUrl, e.getMessage());
            return true;
        }
    }

    private boolean isDisallowed(String robotsContent, String path) {
        boolean inUserAgentBlock = false;
        for (String line : robotsContent.split("\n")) {
            line = line.trim();
            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim();
                inUserAgentBlock = "*".equals(agent);
            } else if (inUserAgentBlock && line.toLowerCase().startsWith("disallow:")) {
                String disallowed = line.substring("disallow:".length()).trim();
                if (!disallowed.isEmpty() && path.startsWith(disallowed)) {
                    return true;
                }
            }
        }
        return false;
    }
}
