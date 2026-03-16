import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useAdminCrawlingSources } from './useAdminCrawlingSources'

const mockAddToast = vi.fn()

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toasts: [],
    addToast: mockAddToast,
    removeToast: vi.fn(),
  }),
}))

const mockFetchCrawlingSources = vi.fn()
const mockCreateCrawlingSource = vi.fn()
const mockUpdateCrawlingSource = vi.fn()
const mockToggleCrawlingSourceActive = vi.fn()
const mockTestCrawlingSource = vi.fn()

vi.mock('@/api/adminApi', () => ({
  fetchCrawlingSources: (...args: unknown[]) => mockFetchCrawlingSources(...args),
  createCrawlingSource: (...args: unknown[]) => mockCreateCrawlingSource(...args),
  updateCrawlingSource: (...args: unknown[]) => mockUpdateCrawlingSource(...args),
  toggleCrawlingSourceActive: (...args: unknown[]) => mockToggleCrawlingSourceActive(...args),
  testCrawlingSource: (...args: unknown[]) => mockTestCrawlingSource(...args),
}))

const sourcesResponse = {
  sources: [
    {
      id: 1, code: 'REVU', name: '레뷰', baseUrl: 'https://www.revu.net',
      listUrlPattern: null, description: null, crawlerType: 'REVU',
      active: true, displayOrder: 1, campaignCount: 20,
      lastCrawledAt: '2026-03-15T10:00:00', createdAt: '2026-03-01T00:00:00',
    },
    {
      id: 2, code: 'MBLE', name: '미블', baseUrl: 'https://www.mble.xyz',
      listUrlPattern: null, description: null, crawlerType: 'MBLE',
      active: false, displayOrder: 2, campaignCount: 10,
      lastCrawledAt: null, createdAt: '2026-03-01T00:00:00',
    },
  ],
  availableCrawlerTypes: ['GANGNAM', 'GENERIC', 'MBLE', 'REVU'],
}

describe('useAdminCrawlingSources', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchCrawlingSources.mockResolvedValue(sourcesResponse)
    mockCreateCrawlingSource.mockResolvedValue({ id: 3, code: 'NEW' })
    mockUpdateCrawlingSource.mockResolvedValue({ id: 1, code: 'REVU' })
    mockToggleCrawlingSourceActive.mockResolvedValue({ id: 1, active: false })
    mockTestCrawlingSource.mockResolvedValue({
      sourceCode: 'REVU', sourceName: '레뷰', crawlerType: 'REVU',
      success: true, totalCount: 3, errorMessage: null,
      items: [{ originalId: '1', title: 'Test 1', originalUrl: 'http://test/1', thumbnailUrl: null, category: 'FOOD' }],
    })
  })

  it('초기 로드 시 소스 목록을 가져온다', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.sources).toHaveLength(2)
    expect(result.current.sources[0].code).toBe('REVU')
    expect(result.current.availableCrawlerTypes).toContain('REVU')
  })

  it('소스 생성 성공 시 토스트 표시', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    let success = false
    await act(async () => {
      success = await result.current.handleCreate({
        code: 'NEW', name: '새소스', baseUrl: 'https://new.com',
        crawlerType: 'GENERIC', displayOrder: 4,
      })
    })

    expect(success).toBe(true)
    expect(mockAddToast).toHaveBeenCalledWith('소스가 생성되었습니다.', 'success')
  })

  it('소스 수정 성공 시 토스트 표시', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    let success = false
    await act(async () => {
      success = await result.current.handleUpdate(1, {
        name: '레뷰 수정', baseUrl: 'https://www.revu.net',
        crawlerType: 'REVU', displayOrder: 1,
      })
    })

    expect(success).toBe(true)
    expect(mockAddToast).toHaveBeenCalledWith('소스가 수정되었습니다.', 'success')
  })

  it('활성/비활성 토글', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.handleToggleActive(1)
    })

    expect(mockAddToast).toHaveBeenCalledWith('상태가 변경되었습니다.', 'success')
  })

  it('테스트 실행 결과를 저장한다', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.handleTest(1)
    })

    expect(result.current.testResult).not.toBeNull()
    expect(result.current.testResult?.success).toBe(true)
    expect(result.current.testResult?.totalCount).toBe(3)
  })

  it('테스트 결과를 초기화할 수 있다', async () => {
    const { result } = renderHook(() => useAdminCrawlingSources())

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.handleTest(1)
    })

    expect(result.current.testResult).not.toBeNull()

    act(() => {
      result.current.clearTestResult()
    })

    expect(result.current.testResult).toBeNull()
  })
})
