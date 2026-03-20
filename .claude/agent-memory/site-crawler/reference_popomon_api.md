---
name: popomon_api
description: Popomon(popomon.com) 캠페인 목록 API 엔드포인트, 파라미터, 응답 필드 정보
type: reference
---

## Popomon 캠페인 API

### 사이트 특성
- Next.js 기반 SSR + CSR 하이브리드 (React Server Components 사용)
- HTML에는 캠페인 데이터가 포함되지 않음 -- 클라이언트에서 API fetch
- Jsoup HTML 파싱 불가, **반드시 JSON API 직접 호출** 필요

### 캠페인 목록 API
- **URL**: `https://popomon.com/api_p/campaign/fetch_getcampaignlist`
- **Method**: GET
- **페이지네이션**: `?pageNum=2` (1-indexed, 기본 12개/페이지)
- **총 캠페인 수**: 응답의 `data.campCount` 필드
- **인증 불필요**: 공개 API

### 캠페인 상세 URL 패턴
- `https://popomon.com/campaign/{C_idx}`
- 예: `https://popomon.com/campaign/226472`

### 응답 구조
```json
{
  "data": {
    "contentsData": [...],
    "campCount": "3540",
    "pageNum": null
  },
  "code": 200,
  "success": true
}
```

### 주요 필드 매핑 (contentsData 배열 각 항목)
| API 필드 | 설명 | CrawledCampaign 매핑 |
|---|---|---|
| C_idx | 캠페인 고유 ID | originalId |
| C_title | 제목 (지역포함 예: [경기/수원시]제이라) | title |
| C_provision | 제공 내역 | provision |
| C_provision_price | 제공 가격 (원) | - |
| transformedPrice | 가격 텍스트 (예: 4만원 상당) | - |
| thumb_img | 썸네일 이미지 URL (CloudFront) | thumbnailUrl |
| C_thumb_img_path | 대체 썸네일 (일부 빈 값) | - |
| C_recruit_type | visiting/shipping/reporting | - |
| C_recruit_type__text | 방문형/배송형/기자단 | - |
| CS_type | BLOG/BLOG_RECEIPT/BLOG_CONTENT/INSTAGRAM/REELS | - |
| CS_type__text | 블로그/블로그+영수증리뷰/인스타그램/릴스 | - |
| CT_type | RESTAURANT/BEAUTY/LEISURE/ROOMS/ETC | category 매핑 |
| C_state | ONGOING 등 | status |
| C_choice_count | 모집 인원 | recruitCount |
| C_volunteer_count | 지원 인원 | - |
| C_regi_start_date | 모집 시작일 (YYYY-MM-DD) | applicationStartDate |
| C_regi_end_date | 모집 종료일 (YYYY-MM-DD) | applicationEndDate |
| C_regi_end_date_count | 마감까지 남은 일수 | - |
| C_emergency_recruit | 긴급 모집 여부 (Y/N) | - |
| C_isPremium | 프리미엄 여부 (Y/N) | - |
| C_provide_reward | 보상 금액 | - |
| isAd | 광고 여부 (Y/N) | - |

### CT_type -> CampaignCategory 매핑
- RESTAURANT -> FOOD
- BEAUTY -> BEAUTY
- LEISURE -> TRAVEL (또는 LIFE)
- ROOMS -> TRAVEL
- ETC -> LIFE
- 기타 -> LIFE

### 이미지 CDN
- 도메인: `d17jwiodubhsh2.cloudfront.net`
- 경로 패턴: `/UPLOAD/CAMPAIGN_THUMB/{timestamp}_{hash}_{username}.jpg`
