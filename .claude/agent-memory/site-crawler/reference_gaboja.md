---
name: gaboja_crawling_reference
description: 가보자체험단 실제 도메인, AJAX API 엔드포인트, HTML 셀렉터, 캠페인 ID 패턴 정보
type: reference
---

## 가보자체험단 (GABOJA) 크롤링 정보

### 도메인
- 실제 URL: `https://www.xn--o39a04kpnjo4k9hgflp.com/` (한글 punycode 도메인)
- `gaboja.com`은 HugeDomains 파킹 도메인 (사이트 없음)
- `gaboja.co.kr`은 무관한 법률상담 사이트

### SSR vs SPA
- SSR 기반 (PHP, nginx/1.24.0, PHP/8.0.30)
- 메인 페이지: 인기캠페인(hit) 섹션은 SSR 렌더링, 나머지(pick/new/closing) 섹션은 AJAX 로드
- `/cmp/` 직접 접근 차단 (alert로 리다이렉트) -- AJAX 엔드포인트 사용 필요

### AJAX 캠페인 목록 API
- URL: `POST /main/ajax/_ajax.cmpMainList.php`
- Content-Type: `application/x-www-form-urlencoded`
- 필수 헤더: `X-Requested-With: XMLHttpRequest`, 세션 쿠키 불필요
- 파라미터: `section={pick|new|closing|hit}&tag={PICK|빈값}`
- 각 섹션별 약 15건 반환 (HTML fragment)

### 개별 캠페인 URL 패턴
- `https://www.xn--o39a04kpnjo4k9hgflp.com/cmp/?id={숫자ID}`
- ID 예시: 333638, 332959, 335824 (6자리 정수, 증가형)

### 썸네일 이미지 패턴
- `/data/file/cmpView/{YYYY}/{MM}/{DD}/thumb-{ID}-{hash}_265x265.{jpg|png}`
- 상대경로이므로 baseUrl 프리픽스 필요

### HTML 셀렉터 (캠페인 아이템)
```
컨테이너:  div.slick-slide
링크:      a.slick_link.type1[href]
썸네일:    .img_area img[src]
제목:      .info_area dl dt
설명:      .info_area dl dd
타입 아이콘: .cate i.{blog|insta|clip|reels}  (블로그/인스타/클립/릴스)
방문유형:  .cate li span  (방문형/배송형)
남은일수:  .cate li:nth-child(2)  (예: "6일 남음")
모집현황:  .current  (예: "신청 104 / 모집 15")
```

### 카테고리 URL 패턴
- 지역 캠페인: `/cmp/?ct1=11` (맛집=1110, 뷰티=1111, 숙박=1112, 문화=1119, 배달=1120, 포장=1122, 기타=1131)
- 제품 캠페인: `/cmp/?ct1=10` (뷰티=1021, 패션=1020, 식품=1022, 생활=1012, 기타=1015)
- 기자단 캠페인: `/cmp/?ct1=14`
- 숏폼 캠페인: `/cmp/?sf=sf`

### data.sql 수정 필요
- 현재 등록된 baseUrl `https://gaboja.com`은 잘못됨
- 올바른 baseUrl: `https://www.xn--o39a04kpnjo4k9hgflp.com`
