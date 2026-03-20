---
name: 4blog.net (포블로그) 크롤링 API 및 구조 분석
description: 4blog.net 캠페인 리스트 API 엔드포인트, JSON 응답 필드, 썸네일 URL 패턴, 카테고리 파라미터 정보
type: reference
---

## 사이트 정보
- 도메인: https://4blog.net (포블로그)
- 렌더링: SSR 기반 (Spring MVC + JSP/Thymeleaf) + AJAX 무한스크롤로 캠페인 리스트 로딩
- 기술스택: jQuery, Bootstrap, Spring Boot (JSESSIONID), Cloudflare CDN

## 캠페인 리스트 API

**엔드포인트**: `GET /loadMoreDataCategory`

**파라미터**:
| 파라미터 | 설명 | 예시 |
|---------|------|------|
| offset | 시작 위치 (0부터) | 0 |
| limit | 한 번에 가져올 개수 | 20 |
| category | SNS 필터 (콤마 구분, 빈값=전체) | blog, instagram, youtube |
| category1 | 유형 필터 | local(방문), deliv(배송), reporter(포스팅) |
| location | 지역 대분류 | |
| location1 | 지역 소분류 | |
| search | 검색어 | |
| bid | 사용자 ID (로그인 시) | |

**최대 로딩**: 300개 (클라이언트 측 maxItems 제한)

**섹션별 API**: `GET /api/sectionCampaigns?type=premium` / `type=favorite`

## JSON 응답 필드

| 필드 | 설명 | 매핑 |
|------|------|------|
| CID | 캠페인 고유 ID (숫자) | originalId |
| CAMPAIGN_NM | 캠페인명 | title |
| PRID | 광고주/프로덕트 ID | 썸네일 URL 구성에 사용 |
| IMGKEY | 이미지 키 (UUID.jpg) | 썸네일 URL 구성에 사용 |
| CATEGORY | SNS 유형 (blog, instagram 등) | category |
| CATEGORY1 | 캠페인 유형 (local/deliv/reporter) | category |
| LOCATION_NM | 지역명 (예: [서울/반포동]) | 위치 정보 |
| REVIEWER_BENEFIT | 제공 내역 | benefit |
| REVIEWER_CNT | 모집 인원 | recruitCount |
| REVIEWER_REQ_CNT | 신청 인원 | |
| REQ_OPEN_DT | 모집 시작일 (MM.dd) | recruitStartDate |
| REQ_CLOSE_DT | 모집 마감일 (MM.dd) | recruitEndDate |
| REVIEW_OPEN_DT | 리뷰 시작일 (MM.dd) | |
| REVIEW_CLOSE_DT | 리뷰 마감일 (MM.dd) | |
| REMAINDATE | 남은 일수 | |
| KEYWORD | 키워드 해시태그 문자열 | keywords |
| PAYBACK | 페이백 금액 | |
| TYPECODE | FREE=셀프등록 캠페인 | |
| SEARCHBOT | Y/N | |

## URL 패턴

- 캠페인 상세: `https://4blog.net/campaign/{CID}/`
- 썸네일 이미지: `https://d3oxv6xcx9d0j1.cloudfront.net/public/pr/{PRID}/thumbnail/{IMGKEY}`
- 리스트 페이지: `/list/all`, `/list/all/local`, `/list/all/deliv`, `/list/all/reporter`, `/list/today`

## 크롤링 시 주의사항
- AJAX API이므로 Jsoup으로 JSON 직접 파싱 가능 (HTML 파싱 불필요)
- 날짜 형식이 MM.dd만 제공되므로 연도 추정 로직 필요
- IMGKEY가 null인 항목은 클라이언트에서 스킵함 (동일하게 처리 필요)
- TYPECODE가 'FREE'인 항목은 셀프등록 캠페인 (일반 체험단과 구분)
