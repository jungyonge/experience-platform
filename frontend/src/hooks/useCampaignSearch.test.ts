import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { useCampaignSearch } from './useCampaignSearch'

vi.mock('@/api/campaignApi', () => ({
  fetchCampaigns: vi.fn(),
}))

import { fetchCampaigns } from '@/api/campaignApi'

const wrapper = ({ children }: { children: ReactNode }) =>
  createElement(MemoryRouter, { initialEntries: ['/'] }, children)

const wrapperWithParams = (search: string) =>
  ({ children }: { children: ReactNode }) =>
    createElement(MemoryRouter, { initialEntries: [`/${search}`] }, children)

const mockResponse = {
  campaigns: [
    {
      id: 1,
      sourceType: 'REVU',
      sourceDisplayName: '레뷰',
      title: '테스트 캠페인',
      thumbnailUrl: null,
      originalUrl: 'https://revu.net/1',
      category: 'FOOD',
      categoryDisplayName: '맛집',
      status: 'RECRUITING',
      statusDisplayName: '모집중',
      recruitCount: 5,
      applyEndDate: '2026-03-31',
    },
  ],
  totalCount: 1,
  totalPages: 1,
  currentPage: 0,
  hasNext: false,
}

describe('useCampaignSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.mocked(fetchCampaigns).mockResolvedValue(mockResponse)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('초기 상태', async () => {
    const { result } = renderHook(() => useCampaignSearch(), { wrapper })

    // 디바운스 타이머 실행
    await act(async () => {
      vi.advanceTimersByTime(300)
    })

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.campaigns).toHaveLength(1)
    expect(result.current.keyword).toBe('')
    expect(result.current.sourceTypes).toEqual([])
    expect(result.current.categories).toEqual([])
    expect(result.current.status).toBe('')
    expect(result.current.sort).toBe('latest')
    expect(result.current.page).toBe(0)
  })

  it('URL 파라미터에서 초기 상태 복원', async () => {
    const customWrapper = wrapperWithParams('?keyword=맛집&sourceTypes=REVU&status=RECRUITING&page=1')

    const { result } = renderHook(() => useCampaignSearch(), { wrapper: customWrapper })

    await act(async () => {
      vi.advanceTimersByTime(300)
    })

    expect(result.current.keyword).toBe('맛집')
    expect(result.current.sourceTypes).toEqual(['REVU'])
    expect(result.current.status).toBe('RECRUITING')
    expect(result.current.page).toBe(1)
  })

  it('필터 토글', async () => {
    const { result } = renderHook(() => useCampaignSearch(), { wrapper })

    await act(async () => {
      vi.advanceTimersByTime(300)
    })

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    act(() => {
      result.current.toggleSourceType('REVU')
    })

    expect(result.current.sourceTypes).toContain('REVU')
    expect(result.current.page).toBe(0)

    // 토글 해제
    act(() => {
      result.current.toggleSourceType('REVU')
    })

    expect(result.current.sourceTypes).not.toContain('REVU')
  })

  it('페이지 변경', async () => {
    const { result } = renderHook(() => useCampaignSearch(), { wrapper })

    await act(async () => {
      vi.advanceTimersByTime(300)
    })

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    act(() => {
      result.current.setPage(2)
    })

    expect(result.current.page).toBe(2)
  })

  it('필터 초기화', async () => {
    const customWrapper = wrapperWithParams('?keyword=test&sourceTypes=REVU&status=RECRUITING')

    const { result } = renderHook(() => useCampaignSearch(), { wrapper: customWrapper })

    await act(async () => {
      vi.advanceTimersByTime(300)
    })

    act(() => {
      result.current.resetFilters()
    })

    expect(result.current.keyword).toBe('')
    expect(result.current.sourceTypes).toEqual([])
    expect(result.current.status).toBe('')
  })
})
