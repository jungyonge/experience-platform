package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crawling")
public class CrawlingProperties {

    private boolean enabled;
    private boolean mockEnabled;
    private String scheduleCron;
    private int maxPagesPerSite;
    private int delayMinMs;
    private int delayMaxMs;
    private int connectionTimeoutMs;
    private int readTimeoutMs;
    private String userAgent;
    private String reveApiToken;
    private String reveUsername;
    private String revePassword;
    private boolean detailFetchEnabled = true;
    private int detailFetchDelayMs = 500;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public int getMaxPagesPerSite() { return maxPagesPerSite; }
    public void setMaxPagesPerSite(int maxPagesPerSite) { this.maxPagesPerSite = maxPagesPerSite; }
    public int getDelayMinMs() { return delayMinMs; }
    public void setDelayMinMs(int delayMinMs) { this.delayMinMs = delayMinMs; }
    public int getDelayMaxMs() { return delayMaxMs; }
    public void setDelayMaxMs(int delayMaxMs) { this.delayMaxMs = delayMaxMs; }
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReveApiToken() { return reveApiToken; }
    public void setReveApiToken(String reveApiToken) { this.reveApiToken = reveApiToken; }
    public String getReveUsername() { return reveUsername; }
    public void setReveUsername(String reveUsername) { this.reveUsername = reveUsername; }
    public String getRevePassword() { return revePassword; }
    public void setRevePassword(String revePassword) { this.revePassword = revePassword; }
    public boolean isDetailFetchEnabled() { return detailFetchEnabled; }
    public void setDetailFetchEnabled(boolean detailFetchEnabled) { this.detailFetchEnabled = detailFetchEnabled; }
    public int getDetailFetchDelayMs() { return detailFetchDelayMs; }
    public void setDetailFetchDelayMs(int detailFetchDelayMs) { this.detailFetchDelayMs = detailFetchDelayMs; }
}
