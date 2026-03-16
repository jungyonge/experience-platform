package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsoupClient {

    private final CrawlingProperties properties;

    public JsoupClient(CrawlingProperties properties) {
        this.properties = properties;
    }

    public Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getConnectionTimeoutMs())
                    .maxBodySize(0)
                    .referrer("https://www.google.com")
                    .get();
        } catch (IOException e) {
            throw new CrawlingException("페이지 조회 실패: " + url, e);
        }
    }
}
