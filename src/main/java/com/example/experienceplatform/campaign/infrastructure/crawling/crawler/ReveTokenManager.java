package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingException;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ReveTokenManager {

    private static final Logger log = LoggerFactory.getLogger(ReveTokenManager.class);
    private static final String TOKEN_URL = "https://api.weble.net/tokens";

    private final CrawlingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String cachedToken;

    public ReveTokenManager(CrawlingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public synchronized String getToken() {
        if (cachedToken != null) {
            return cachedToken;
        }

        String configToken = properties.getReveApiToken();
        if (configToken != null && !configToken.isBlank()) {
            cachedToken = configToken;
            return cachedToken;
        }

        return refreshToken();
    }

    public synchronized String refreshToken() {
        String username = properties.getReveUsername();
        String password = properties.getRevePassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("REVE 로그인 정보가 설정되지 않았습니다. crawling.reve-username, crawling.reve-password 설정을 확인하세요.");
            return null;
        }

        try {
            String requestBody = objectMapper.writeValueAsString(new ReveLoginRequest(username, password, true));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new CrawlingException("REVE 토큰 발급 실패: HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String token = root.path("token").asText(null);
            if (token == null) {
                token = root.path("access_token").asText(null);
            }
            if (token == null) {
                token = root.asText(null);
            }

            if (token != null && !token.isBlank()) {
                cachedToken = token;
                log.info("REVE JWT 토큰 갱신 완료");
                return cachedToken;
            }

            throw new CrawlingException("REVE 토큰 응답에서 토큰을 찾을 수 없습니다: " + response.body());
        } catch (CrawlingException e) {
            throw e;
        } catch (Exception e) {
            throw new CrawlingException("REVE 토큰 발급 중 오류: " + e.getMessage(), e);
        }
    }

    public synchronized void invalidateToken() {
        cachedToken = null;
    }

    private record ReveLoginRequest(String username, String password, boolean remember) {}
}
