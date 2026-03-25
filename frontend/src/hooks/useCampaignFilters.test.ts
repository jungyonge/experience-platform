import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useCampaignFilters } from './useCampaignFilters'

vi.mock('@/api/campaignApi', () => ({
  fetchFilterOptions: vi.fn(),
}))

import { fetchFilterOptions } from '@/api/campaignApi'

describe('useCampaignFilters', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('필터 옵션 로드', async () => {
    const mockFilters = {
      sourceTypes: [{ code: 'REVU', name: '레뷰' }],
      categories: [{ code: 'FOOD', name: '맛집' }],
      statuses: [{ code: 'RECRUITING', name: '모집중' }],
      sortOptions: [{ code: 'latest', name: '최신순' }],
      regions: [{ sido: '서울특별시', sigungus: [{ code: '1', name: '강남구' }] }],
    }
    vi.mocked(fetchFilterOptions).mockResolvedValue(mockFilters)

    const { result } = renderHook(() => useCampaignFilters())

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.filters).toEqual(mockFilters)
    expect(fetchFilterOptions).toHaveBeenCalledTimes(1)
  })

  it('API 호출 실패 시 filters는 null', async () => {
    vi.mocked(fetchFilterOptions).mockRejectedValue(new Error('fail'))

    const { result } = renderHook(() => useCampaignFilters())

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.filters).toBeNull()
  })
})
