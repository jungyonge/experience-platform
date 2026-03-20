---
name: Stylec API
description: 스타일씨(stylec.co.kr) 체험단 캠페인 JSON API 엔드포인트, 필드 매핑, 주의사항
type: reference
---

## API Endpoint

- URL: `https://api2.stylec.co.kr:6439/v1/trial?page={page}&count={count}&order=latest`
- Method: GET
- Required Headers: `Referer: https://www.stylec.co.kr`, `Accept: application/json`

## Response Structure

```
{
  "success": true,
  "message": "정상적으로 처리되었습니다.",
  "data": {
    "data": [ ...items ],
    "Total": 79772          // 주의: 대문자 T
  }
}
```

## Item Fields (주요)

| 필드 | 설명 | 예시 |
|---|---|---|
| wr_id | 캠페인 고유 ID | 5869 |
| wr_subject | 제목 | "[미션형 체험단] 헤이로지..." |
| img | 썸네일 (CDN 절대 URL) | "https://cdn.stylec.co.kr/data/editors/..." |
| link | 상세 상대경로 | "/trials/5869" |
| sns_type | SNS 유형 | "naverblog", "instagram" |
| ca_name | 카테고리 | "서비스", "제품" |
| wr_type | 체험단 유형 | "기자단", "제공" |
| tr_recruit_max | 모집 인원 | 30 |
| tr_enroll_cnt | 신청 인원 | 63 |
| tr_recruit_start | 모집 시작일 | "2023-09-06 00:00:00" |
| tr_recruit_finish | 모집 마감일 | "2023-09-10 23:59:59" |
| dday | D-day (음수=마감) | -921 |
| tr_cashback_amt | 캐시백 금액 | 2000 또는 "0" (타입 불일치 주의) |

## 주의사항

- `data.Total` 대문자 T (소문자 아님)
- `tr_cashback_amt` 타입 불일치: number일 때도 있고 string일 때도 있음
- `link`는 상대경로 -> 절대 URL: `https://www.stylec.co.kr` + link
- 전체 키(27개): link, img, it_id, wr_subject, sns_type, ca_name, wr_type, wr_type_label, tr_it_cate_id, dday, enroll_dday, finday, diffdays, page, tr_recruit_max, tr_enroll_cnt, tr_owner, tr_it_detail_html, tr_cashback_amt, it_price, tr_cashback_curr, tr_plus_certify, wr_id, campaign_id, dev_status, priority, tr_adult, wr_8, office_confirm, tr_recruit_start, tr_recruit_finish, startTomorrow, wr_datetime
