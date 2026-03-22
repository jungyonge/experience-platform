package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DetailPageEnricher {

    private static final Logger log = LoggerFactory.getLogger(DetailPageEnricher.class);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;

    public DetailPageEnricher(CrawlingProperties properties, JsoupClient jsoupClient) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
    }

    public List<CrawledCampaign> enrich(List<CrawledCampaign> campaigns,
                                         BiFunction<CrawledCampaign, Document, CrawledCampaign> parser) {
        if (!properties.isDetailFetchEnabled()) {
            return campaigns;
        }
        List<CrawledCampaign> enriched = new ArrayList<>();
        for (CrawledCampaign campaign : campaigns) {
            try {
                Thread.sleep(properties.getDetailFetchDelayMs());
                Document detailDoc = jsoupClient.fetch(campaign.getOriginalUrl());
                CrawledCampaign enrichedCampaign = parser.apply(campaign, detailDoc);
                enriched.add(enrichedCampaign != null ? enrichedCampaign : campaign);
            } catch (Exception e) {
                log.warn("상세페이지 fetch 실패 {}: {}", campaign.getOriginalId(), e.getMessage());
                enriched.add(campaign);
            }
        }
        return enriched;
    }

    public static String coalesce(String existing, String fallback) {
        return existing != null ? existing : fallback;
    }

    public static Integer coalesce(Integer existing, Integer fallback) {
        return existing != null ? existing : fallback;
    }

    public static java.time.LocalDate coalesce(java.time.LocalDate existing, java.time.LocalDate fallback) {
        return existing != null ? existing : fallback;
    }

    // ── 공통 상세 페이지 추출 메서드 ──

    private static final Pattern APPLICANTS_PATTERN = Pattern.compile("신청\\s*(\\d[\\d,]*)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d[\\d,]*)");
    private static final Pattern DATE_YYYY_MM_DD = Pattern.compile("(\\d{4})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})");
    private static final Pattern DATE_MM_DD = Pattern.compile("(\\d{1,2})[.\\-/](\\d{1,2})");
    private static final String[] DETAIL_CONTENT_SELECTORS = {
            "#tab1", ".detail-content", ".campaign-detail", ".view_content", "#content",
            ".cont_area", ".detail_wrap", ".campaign_detail", ".camp_detail",
            ".detail_info", ".campaign-content", ".review-detail", ".campaign_view",
            ".board_view_content", ".board_content", ".entry-content",
            "article .content", "section.detail", ".detail-body"
    };
    private static final String[] ADDRESS_LABELS = {"주소", "위치", "업체주소", "방문주소", "매장주소", "장소"};
    private static final String[] ADDRESS_CSS_SELECTORS = {
            ".address", ".addr", ".location",
            "[class*=address]", "[class*=addr]", "[class*=location]",
            "[data-address]", ".store-address", ".shop-address",
            ".place-address", ".campaign-address"
    };
    private static final Pattern KOREAN_ADDRESS_PATTERN = Pattern.compile(
            "(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)" +
            "(?:특별시|광역시|특별자치시|특별자치도|도)?" +
            "\\s*[가-힣]{1,10}(?:시|군|구|동|읍|면)" +
            "[\\s가-힣0-9\\-.,()]*"
    );
    private static final Pattern ADDRESS_FALSE_POSITIVE = Pattern.compile(
            ".{0,3}맛집|.{0,3}여행|.{0,3}체험|.{0,3}투어|.{0,3}핫플|에서\\s|" +
            "주세요|작성|리뷰|소개|텍스트|링크|미션|필수|사진|포스팅"
    );
    private static final String ADDRESS_CANDIDATE_ELEMENTS = "p, div, span, li, td, dd, address";
    private static final String[] ANNOUNCE_LABELS = {"선정발표", "발표일", "당첨자 발표", "선정자 발표", "결과발표", "당첨발표"};
    private static final String[] APPLY_START_LABELS = {"모집기간", "신청기간", "접수기간", "모집 기간", "신청 기간"};

    /**
     * 상세 콘텐츠 영역 추출 — 여러 셀렉터를 순차 시도
     */
    public static String extractDetailContent(Document doc) {
        for (String sel : DETAIL_CONTENT_SELECTORS) {
            Element el = doc.selectFirst(sel);
            if (el != null && el.html().trim().length() > 50) {
                return el.html();
            }
        }
        // fallback: main 또는 article 영역
        Element main = doc.selectFirst("main");
        if (main == null) main = doc.selectFirst("article");
        if (main != null && main.html().trim().length() > 100) {
            return main.html();
        }
        return null;
    }

    /**
     * 현재 신청자 수 추출 — "신청 N" 패턴
     */
    public static Integer extractCurrentApplicants(Document doc) {
        Matcher m = APPLICANTS_PATTERN.matcher(doc.text());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) { /* ignore */ }
        }
        return null;
    }

    /**
     * 발표일 추출 — "선정발표", "발표일" 등의 라벨 근처 날짜
     */
    public static LocalDate extractAnnouncementDate(Document doc) {
        String fullText = doc.text();
        for (String label : ANNOUNCE_LABELS) {
            int idx = fullText.indexOf(label);
            if (idx >= 0) {
                String after = fullText.substring(idx, Math.min(idx + 80, fullText.length()));
                LocalDate date = parseDateFromContext(after);
                if (date != null) return date;
            }
        }
        // 라벨 기반 탐색: th/dt/label 등
        for (Element el : doc.select("th, dt, .label, .tit, .tit_basic, strong, b, span")) {
            String text = el.text().trim();
            for (String label : ANNOUNCE_LABELS) {
                if (text.contains(label)) {
                    Element sibling = el.nextElementSibling();
                    if (sibling != null) {
                        LocalDate date = parseDateFromContext(sibling.text());
                        if (date != null) return date;
                    }
                    // 부모에서 찾기
                    if (el.parent() != null) {
                        LocalDate date = parseDateFromContext(el.parent().text());
                        if (date != null) return date;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 신청 시작일 추출 — "모집기간", "신청기간" 라벨의 첫 번째 날짜
     */
    public static LocalDate extractApplyStartDate(Document doc) {
        String fullText = doc.text();
        for (String label : APPLY_START_LABELS) {
            int idx = fullText.indexOf(label);
            if (idx >= 0) {
                String after = fullText.substring(idx, Math.min(idx + 100, fullText.length()));
                LocalDate date = parseDateFromContext(after);
                if (date != null) return date;
            }
        }
        // 라벨 기반 탐색
        for (Element el : doc.select("th, dt, .label, .tit, .tit_basic, strong, b, span")) {
            String text = el.text().trim();
            for (String label : APPLY_START_LABELS) {
                if (text.contains(label)) {
                    Element sibling = el.nextElementSibling();
                    if (sibling != null) {
                        LocalDate date = parseDateFromContext(sibling.text());
                        if (date != null) return date;
                    }
                    if (el.parent() != null) {
                        LocalDate date = parseDateFromContext(el.parent().text());
                        if (date != null) return date;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 주소 추출 — 5단계 fallback 전략
     * A: 라벨 기반 형제 요소 탐색
     * B: Tanzsoft ul.basic_form 패턴
     * C: CSS 클래스/속성 기반 직접 검색
     * D: 상세 콘텐츠 영역에서 한국 주소 정규식 패턴 매칭
     * E: 지도 링크 주변 텍스트
     */
    public static String extractAddress(Document doc) {
        // ── 전략 A: 라벨 기반 탐색 ──
        for (Element el : doc.select("th, dt, .label, .tit, .tit_basic, strong, b, span, p")) {
            String text = el.text().trim();
            for (String label : ADDRESS_LABELS) {
                if (!text.contains(label)) continue;
                // 라벨이 별도 요소 (짧은 텍스트)인 경우: 형제/부모에서 주소 추출
                if (text.length() < 20) {
                    Element sibling = el.nextElementSibling();
                    if (sibling != null) {
                        String addr = sibling.text().trim();
                        if (addr.length() >= 5 && addr.length() <= 200) return addr;
                    }
                    if (el.parent() != null) {
                        Element parentSibling = el.parent().nextElementSibling();
                        if (parentSibling != null) {
                            String addr = parentSibling.text().trim();
                            if (addr.length() >= 5 && addr.length() <= 200) return addr;
                        }
                        Element content = el.parent().selectFirst(".content, .con, td, dd");
                        if (content != null) {
                            String addr = content.text().trim();
                            if (addr.length() >= 5 && addr.length() <= 200) return addr;
                        }
                    }
                }
                // 라벨과 주소가 같은 요소에 합쳐진 경우: "매장주소 : 서울 강남구..."
                if (text.length() >= 10 && text.length() <= 200) {
                    String afterLabel = text.substring(text.indexOf(label) + label.length())
                            .replaceFirst("^\\s*:?\\s*", "").trim();
                    if (afterLabel.length() >= 5 && afterLabel.length() <= 150
                            && afterLabel.matches(".*(?:시|군|구|동|로|길|읍|면).*")) {
                        return afterLabel;
                    }
                }
            }
        }

        // ── 전략 B: Tanzsoft ul.basic_form 패턴 ──
        for (Element li : doc.select("ul.basic_form li")) {
            Element titEl = li.selectFirst(".tit_basic");
            if (titEl != null) {
                for (String label : ADDRESS_LABELS) {
                    if (titEl.text().contains(label)) {
                        Element contentEl = li.selectFirst(".content");
                        if (contentEl != null) {
                            String addr = contentEl.text().trim();
                            if (addr.length() >= 5) return addr;
                        }
                    }
                }
            }
        }

        // ── 전략 C: CSS 클래스/속성 기반 직접 검색 ──
        for (String selector : ADDRESS_CSS_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String dataAddr = el.attr("data-address");
                if (!dataAddr.isEmpty() && dataAddr.length() >= 5 && dataAddr.length() <= 200) {
                    return dataAddr;
                }
                String addr = el.text().trim();
                if (addr.length() >= 5 && addr.length() <= 200
                        && containsKoreanAddressComponent(addr)) {
                    return addr;
                }
            }
        }

        // ── 전략 D: 상세 콘텐츠 영역에서 한국 주소 패턴 검색 ──
        Element contentArea = findContentArea(doc);
        if (contentArea != null) {
            String found = findAddressByPattern(contentArea);
            if (found != null) return found;
        }

        // ── 전략 E: 지도 링크 주변 텍스트 ──
        for (Element a : doc.select("a[href*=map.naver], a[href*=map.kakao], a[href*=place.naver], a[href*=place.map]")) {
            String linkText = a.text().trim();
            if (linkText.length() >= 8 && linkText.length() <= 200
                    && KOREAN_ADDRESS_PATTERN.matcher(linkText).find()) {
                return linkText;
            }
            if (a.parent() != null) {
                String parentText = a.parent().text().trim();
                Matcher m = KOREAN_ADDRESS_PATTERN.matcher(parentText);
                if (m.find() && m.group().length() >= 8) {
                    return m.group().trim();
                }
            }
        }

        return null;
    }

    private static Element findContentArea(Document doc) {
        for (String sel : DETAIL_CONTENT_SELECTORS) {
            Element el = doc.selectFirst(sel);
            if (el != null && el.text().trim().length() > 50) {
                return el;
            }
        }
        Element main = doc.selectFirst("main");
        if (main == null) main = doc.selectFirst("article");
        if (main == null) main = doc.body();
        return main;
    }

    private static String findAddressByPattern(Element contentArea) {
        // 1차: ownText()로 탐색 (컨테이너 중복 방지)
        for (Element el : contentArea.select(ADDRESS_CANDIDATE_ELEMENTS)) {
            String ownText = el.ownText().trim();
            if (ownText.length() < 8 || ownText.length() > 300) continue;

            Matcher m = KOREAN_ADDRESS_PATTERN.matcher(ownText);
            if (m.find()) {
                String candidate = m.group().trim();
                if (candidate.length() >= 8 && candidate.length() <= 200
                        && !isFalsePositive(candidate)) {
                    return candidate;
                }
            }
        }
        // 2차: text()로 leaf 요소 탐색 (ownText가 비어있는 중첩 구조 대응)
        for (Element el : contentArea.select(ADDRESS_CANDIDATE_ELEMENTS)) {
            if (!el.children().isEmpty() && el.children().size() > 3) continue;
            String fullText = el.text().trim();
            if (fullText.length() < 8 || fullText.length() > 80) continue;

            Matcher m = KOREAN_ADDRESS_PATTERN.matcher(fullText);
            if (m.find()) {
                String candidate = m.group().trim();
                if (candidate.length() >= 8 && candidate.length() <= 200
                        && !isFalsePositive(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static final String[] PROVINCE_NAMES = {
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
    };

    private static boolean isFalsePositive(String candidate) {
        if (ADDRESS_FALSE_POSITIVE.matcher(candidate).find()) {
            return true;
        }
        // 시/도명이 2개 이상 포함되면 지역 목록(네비게이션)으로 판단
        int provinceCount = 0;
        for (String p : PROVINCE_NAMES) {
            if (candidate.contains(p)) provinceCount++;
        }
        if (provinceCount >= 2) {
            return true;
        }
        // 시/군/구 포함 시 유효 주소로 인정
        if (candidate.matches(".*(시|군|구).*")) {
            return false;
        }
        // 시/군/구 없으면 숫자 또는 동/로/길 등 주소 구성요소 필요
        return !candidate.matches(".*[0-9].*")
                && !candidate.matches(".*(동|로|길|번지|층|호|읍|면|리).*");
    }

    private static boolean containsKoreanAddressComponent(String text) {
        return text.matches(".*(시|군|구|동|로|길|읍|면|리).*");
    }

    /**
     * 텍스트에서 날짜 파싱 (yyyy.MM.dd 또는 MM.dd)
     */
    private static LocalDate parseDateFromContext(String text) {
        if (text == null || text.isBlank()) return null;
        // yyyy-MM-dd 형식 먼저 시도
        Matcher m = DATE_YYYY_MM_DD.matcher(text);
        if (m.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                );
            } catch (Exception e) { /* ignore */ }
        }
        // MM.dd 형식 (현재 연도 사용)
        Matcher m2 = DATE_MM_DD.matcher(text);
        if (m2.find()) {
            try {
                int month = Integer.parseInt(m2.group(1));
                int day = Integer.parseInt(m2.group(2));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return LocalDate.now().withMonth(month).withDayOfMonth(day);
                }
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }
}
