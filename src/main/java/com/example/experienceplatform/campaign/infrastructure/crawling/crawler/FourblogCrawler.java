package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class FourblogCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(FourblogCrawler.class);
    private static final String BASE_URL = "https://4blog.net";
    private static final String API_URL = BASE_URL + "/loadMoreDataCategory";
    private static final String CDN_URL = "https://d3oxv6xcx9d0j1.cloudfront.net/public/pr/";
    private static final Pattern ADDRESS_FROM_TITLE = Pattern.compile(
            "\\[((?:서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)[^\\]]*)]");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public FourblogCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                            CrawlingDelayHandler delayHandler, ObjectMapper objectMapper,
                            DetailPageEnricher enricher) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.enricher = enricher;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCrawlerType() {
        return "FOURBLOG";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();

        try {
            // 1. 메인 페이지에서 세션 쿠키 + CSRF 토큰 획득
            org.jsoup.Connection.Response mainResponse = org.jsoup.Jsoup.connect(BASE_URL + "/?category=blog")
                    .userAgent(properties.getUserAgent())
                    .timeout((int) properties.getReadTimeoutMs())
                    .execute();
            java.util.Map<String, String> cookies = mainResponse.cookies();

            org.jsoup.nodes.Document mainDoc = mainResponse.parse();
            org.jsoup.nodes.Element csrfMeta = mainDoc.selectFirst("meta[name=_csrf]");
            org.jsoup.nodes.Element csrfHeaderMeta = mainDoc.selectFirst("meta[name=_csrf_header]");
            String csrfToken = csrfMeta != null ? csrfMeta.attr("content") : "";
            String csrfHeader = csrfHeaderMeta != null ? csrfHeaderMeta.attr("content") : "X-CSRF-TOKEN";

            // 2. AJAX API 호출 (세션 쿠키 + CSRF 포함)
            String apiUrl = API_URL + "?offset=0&limit=300&category=blog&category1=&location=&search=";
            org.jsoup.Connection.Response apiResponse = org.jsoup.Jsoup.connect(apiUrl)
                    .cookies(cookies)
                    .header(csrfHeader, csrfToken)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .userAgent(properties.getUserAgent())
                    .timeout((int) properties.getReadTimeoutMs())
                    .ignoreContentType(true)
                    .execute();

            if (apiResponse.statusCode() != 200) {
                log.error("FOURBLOG API 응답 오류: HTTP {}", apiResponse.statusCode());
                return results;
            }

            String body = apiResponse.body();
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root;
            if (!items.isArray()) items = root.path("data");
            if (!items.isArray() || items.isMissingNode()) {
                log.warn("FOURBLOG 예상하지 못한 응답 구조: {}", body.substring(0, Math.min(200, body.length())));
                return results;
            }

            for (JsonNode item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("FOURBLOG 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("FOURBLOG 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("FOURBLOG 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        String description = null;
        Element metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        String detailContent = DetailPageEnricher.extractDetailContent(doc);
        Integer currentApplicants = DetailPageEnricher.extractCurrentApplicants(doc);
        LocalDate announcementDate = DetailPageEnricher.extractAnnouncementDate(doc);
        LocalDate applyStartDate = DetailPageEnricher.extractApplyStartDate(doc);
        String address = DetailPageEnricher.extractAddress(doc);

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(),
                coalesce(campaign.getApplyStartDate(), applyStartDate),
                campaign.getApplyEndDate(),
                coalesce(campaign.getAnnouncementDate(), announcementDate),
                campaign.getReward(), campaign.getMission(),
                coalesce(campaign.getAddress(), address),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        String cid = item.path("CID").asText("");
        String title = item.path("CAMPAIGN_NM").asText("").trim();
        if (cid.isEmpty() || title.isEmpty()) return null;

        String originalUrl = BASE_URL + "/campaign/" + cid + "/";

        String prid = item.path("PRID").asText("");
        String imgKey = item.path("IMGKEY").asText("");
        String thumbnailUrl = (prid.isEmpty() || imgKey.isEmpty()) ? null :
                CDN_URL + prid + "/thumbnail/" + imgKey;

        int reviewerCnt = item.path("REVIEWER_CNT").asInt(0);
        String benefit = item.path("REVIEWER_BENEFIT").asText(null);
        String category1 = item.path("CATEGORY1").asText("");
        String keyword = item.path("KEYWORD").asText("");

        int remainDate = item.path("REMAINDATE").asInt(0);
        LocalDate applyEndDate = remainDate > 0 ? LocalDate.now().plusDays(remainDate) : null;
        CampaignStatus status = remainDate >= 0 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;

        CampaignCategory category = CategoryMapper.map(title + " " + category1 + " " + keyword);

        // 제목에서 주소 추출: "[서울/송파] 매장명" → "서울 송파"
        String address = null;
        Matcher addrMatcher = ADDRESS_FROM_TITLE.matcher(title);
        if (addrMatcher.find()) {
            address = addrMatcher.group(1).replace("/", " ").trim();
        }

        return new CrawledCampaign(
                source.getCode(), cid, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                reviewerCnt > 0 ? reviewerCnt : null, null, applyEndDate, null,
                benefit, "블로그 리뷰 작성", address, "포블로그,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "fourblog-" + i,
                    "[포블로그] 체험단 캠페인 #" + i,
                    "포블로그 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=FOURBLOG+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",포블로그,체험단"
            ));
        }
        return mocks;
    }
}
