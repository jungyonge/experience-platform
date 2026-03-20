---
name: ReviewExpedition (리뷰원정대) 사이트 크롤링 정보
description: 리뷰원정대 실제 도메인, JSON API 엔드포인트, 응답 필드, URL 패턴 정보
type: reference
---

## 도메인 정보
- DB 등록 URL: `https://reviewexpedition.co.kr` (DNS NXDOMAIN - 해석 불가)
- 실제 도메인: `https://xn--vk1bn0kvydxrlprb.com` (퓨니코드, 한글: 리뷰원정대.com)
- 플랫폼: 커스텀 PHP (nfor 기반), nginx, PHPSESSID

## JSON API (HTML 파싱 불필요)
- 엔드포인트: `GET campaign_list.php?json=list&category_id=&keyword=&cp_media=&orderby=cp_id&page={page}`
- 전체 데이터를 page=1 한 번에 반환 (last_page=1, 2026-03-20 기준 176건)
- 페이지네이션: AJAX 무한스크롤 방식이나 실제로는 1페이지에 전체 반환

### 응답 필드
| 필드 | 설명 | 예시 |
|------|------|------|
| cp_id | 캠페인 고유 ID | "1773987484" |
| cp_subject | 캠페인 제목 | "속 건조를 잡아주는 에센스!" |
| cp_description | 업체/브랜드명 | "[쿠팡]베이지크" |
| cp_img | 썸네일 (상대경로) | "./data/campaign/thumb/thumb-xxx_300x300.jpg" |
| cp_category_text | 카테고리 | "배송/뷰티" |
| cp_type | 캠페인 유형 | "배송형", "방문형" |
| cp_recruit | 모집인원 | "10" |
| cp_day | 남은기간 | "D-Day 7" |
| cp_point | 포인트 | "48,000" 또는 "0" |
| cp_media_blog/instagram/youtube/shop/reels/shorts/tiktok/clip | 미디어 타입 플래그 | "0" 또는 "1" |
| cp_review | 리뷰 수 | "0" |
| cp_order | 신청자 수 | "8" |

### URL 패턴
- 목록: `campaign_list.php?category_id={카테고리}`
- 개별: `campaign.php?cp_id={cp_id}`
- originalId = cp_id
- originalUrl = `https://xn--vk1bn0kvydxrlprb.com/campaign.php?cp_id={cp_id}`
- thumbnailUrl = `https://xn--vk1bn0kvydxrlprb.com/{cp_img}` (상대경로 -> 절대경로 변환 필요)

### 카테고리 필터 (category_id)
- 전용관: 337A, 배송: 002A, 숏폼: 224A, 구매평: 003A, 방문: 001A
- 하위 카테고리: 002A005A(식품), 002A021A(뷰티), 001A007A(서울) 등

### cp_media 필터
- blog, instagram, youtube, tiktok, shop, clip

## HTML 셀렉터 (SSR 부분, 초기 로딩 아이템)
- 아이템 컨테이너: `div.item_box_list_zone`
- 제목: `span.it_name`
- 설명: `span.it_description`
- 썸네일: `div.thumb img.it_img`
- 링크: `a[href*="campaign.php?cp_id="]`
- D-Day: `span.dday`
- 모집인원: `div.option span` (두번째)
- 카테고리: `span.area_zone`
- 타입: `span.delivery_btn`
- 포인트: `div.point_box b` (두번째)
- 미디어: `span.blog`, `span.shop`, `span.instagram` 등

## 크롤러 개선 사항
기존 ReviewExpeditionCrawler는 gnuboard 기반 `/bbs/board.php?bo_table=campaign` 패턴 사용 중 (404 반환).
JSON API 방식으로 전면 교체 필요.
