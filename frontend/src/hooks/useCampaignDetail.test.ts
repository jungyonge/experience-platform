import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useCampaignDetail } from './useCampaignDetail'
import * as campaignApi from '@/api/campaignApi'
import type { CampaignDetailItem } from '@/types/campaign'

vi.mock('@/api/campaignApi')

const mockCampaign: CampaignDetailItem = {
  id: 1,
  sourceType: 'REVU',
  sourceDisplayName: '레뷰',
  title: '테스트 캠페인',
  description: '설명',
  detailContent: '상세 설명',
  thumbnailUrl: 'https://thumb.jpg',
  originalUrl: 'https://revu.net/1001',
  category: 'FOOD',
  categoryDisplayName: '맛집',
  status: 'RECRUITING',
  statusDisplayName: '모집중',
  recruitCount: 5,
  currentApplicants: null,
  applyStartDate: '2026-03-01',
  applyEndDate: '2026-03-31',
  announcementDate: '2026-04-03',
  reward: '2인 식사권',
  mission: '블로그 리뷰 작성',
  address: '서울 강남구',
  keywords: ['강남맛집', '이탈리안'],
  createdAt: '2026-03-10T10:00:00',
  updatedAt: '2026-03-10T10:00:00',
}

describe('useCampaignDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('정상 데이터 로드', async () => {
    vi.mocked(campaignApi.fetchCampaignDetail).mockResolvedValue(mockCampaign)

    const { result } = renderHook(() => useCampaignDetail('1'))

    expect(result.current.isLoading).toBe(true)

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.campaign).toEqual(mockCampaign)
    expect(result.current.isNotFound).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('404 응답 → isNotFound = true', async () => {
    vi.mocked(campaignApi.fetchCampaignDetail).mockRejectedValue({
      code: 'CAMPAIGN_NOT_FOUND',
      message: '캠페인을 찾을 수 없습니다.',
    })

    const { result } = renderHook(() => useCampaignDetail('999'))

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.isNotFound).toBe(true)
    expect(result.current.campaign).toBeNull()
    expect(result.current.error).toBeNull()
  })

  it('네트워크 에러 → error', async () => {
    vi.mocked(campaignApi.fetchCampaignDetail).mockRejectedValue(new Error('Network Error'))

    const { result } = renderHook(() => useCampaignDetail('1'))

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.error).toBeTruthy()
    expect(result.current.campaign).toBeNull()
    expect(result.current.isNotFound).toBe(false)
  })

  it('retry 동작', async () => {
    vi.mocked(campaignApi.fetchCampaignDetail)
      .mockRejectedValueOnce(new Error('Network Error'))
      .mockResolvedValueOnce(mockCampaign)

    const { result } = renderHook(() => useCampaignDetail('1'))

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.error).toBeTruthy()

    // retry
    result.current.retry()

    await waitFor(() => {
      expect(result.current.campaign).toEqual(mockCampaign)
    })

    expect(result.current.error).toBeNull()
  })

  it('잘못된 id 형식 → isNotFound', async () => {
    const { result } = renderHook(() => useCampaignDetail('abc'))

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.isNotFound).toBe(true)
    expect(campaignApi.fetchCampaignDetail).not.toHaveBeenCalled()
  })

  it('undefined id → isNotFound', async () => {
    const { result } = renderHook(() => useCampaignDetail(undefined))

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.isNotFound).toBe(true)
    expect(campaignApi.fetchCampaignDetail).not.toHaveBeenCalled()
  })
})
