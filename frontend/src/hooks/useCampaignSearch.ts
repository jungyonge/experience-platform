import { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchCampaigns } from '@/api/campaignApi'
import type { CampaignItem } from '@/types/campaign'

interface SearchState {
  campaigns: CampaignItem[]
  totalCount: number
  totalPages: number
  currentPage: number
  hasNext: boolean
  isLoading: boolean
  error: string | null
}

function parseSet(value: string | null): string[] {
  if (!value) return []
  return value.split(',').filter(Boolean)
}

export function useCampaignSearch() {
  const [searchParams, setSearchParams] = useSearchParams()

  const keyword = searchParams.get('keyword') || ''
  const sourceTypes = parseSet(searchParams.get('sourceTypes'))
  const categories = parseSet(searchParams.get('categories'))
  const status = searchParams.get('status') || ''
  const regionId = searchParams.get('regionId') || ''
  const sido = searchParams.get('sido') || ''
  const page = Number(searchParams.get('page') || '0')
  const size = Number(searchParams.get('size') || '12')
  const sort = searchParams.get('sort') || 'latest'

  const [state, setState] = useState<SearchState>({
    campaigns: [],
    totalCount: 0,
    totalPages: 0,
    currentPage: 0,
    hasNext: false,
    isLoading: true,
    error: null,
  })

  // 디바운스용 keyword
  const [debouncedKeyword, setDebouncedKeyword] = useState(keyword)
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>()

  useEffect(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => {
      setDebouncedKeyword(keyword)
    }, 300)
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current)
    }
  }, [keyword])

  // API 호출
  useEffect(() => {
    let cancelled = false
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    fetchCampaigns({
      keyword: debouncedKeyword || undefined,
      sourceTypes: sourceTypes.length > 0 ? sourceTypes : undefined,
      categories: categories.length > 0 ? categories : undefined,
      status: status || undefined,
      regionId: regionId ? Number(regionId) : undefined,
      sido: !regionId && sido ? sido : undefined,
      page,
      size,
      sort,
    })
      .then((data) => {
        if (cancelled) return
        setState({
          campaigns: data.campaigns,
          totalCount: data.totalCount,
          totalPages: data.totalPages,
          currentPage: data.currentPage,
          hasNext: data.hasNext,
          isLoading: false,
          error: null,
        })
      })
      .catch(() => {
        if (cancelled) return
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: '캠페인 목록을 불러오는데 실패했습니다.',
        }))
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword, sourceTypes.join(','), categories.join(','), status, regionId, sido, page, size, sort])

  const updateParams = useCallback(
    (updates: Record<string, string | undefined>) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        for (const [key, value] of Object.entries(updates)) {
          if (value === undefined || value === '') {
            next.delete(key)
          } else {
            next.set(key, value)
          }
        }
        return next
      })
    },
    [setSearchParams]
  )

  const setKeyword = useCallback(
    (value: string) => {
      updateParams({ keyword: value || undefined, page: '0' })
    },
    [updateParams]
  )

  const toggleSourceType = useCallback(
    (code: string) => {
      const current = new Set(sourceTypes)
      if (current.has(code)) {
        current.delete(code)
      } else {
        current.add(code)
      }
      updateParams({
        sourceTypes: current.size > 0 ? Array.from(current).join(',') : undefined,
        page: '0',
      })
    },
    [sourceTypes, updateParams]
  )

  const toggleCategory = useCallback(
    (code: string) => {
      const current = new Set(categories)
      if (current.has(code)) {
        current.delete(code)
      } else {
        current.add(code)
      }
      updateParams({
        categories: current.size > 0 ? Array.from(current).join(',') : undefined,
        page: '0',
      })
    },
    [categories, updateParams]
  )

  const setStatus = useCallback(
    (value: string) => {
      updateParams({ status: value || undefined, page: '0' })
    },
    [updateParams]
  )

  const setRegion = useCallback(
    (newRegionId: string, newSido: string) => {
      updateParams({
        regionId: newRegionId || undefined,
        sido: newSido || undefined,
        page: '0',
      })
    },
    [updateParams]
  )

  const setSort = useCallback(
    (value: string) => {
      updateParams({ sort: value, page: '0' })
    },
    [updateParams]
  )

  const setPage = useCallback(
    (value: number) => {
      updateParams({ page: String(value) })
    },
    [updateParams]
  )

  const resetFilters = useCallback(() => {
    setSearchParams(new URLSearchParams())
  }, [setSearchParams])

  return {
    ...state,
    keyword,
    sourceTypes,
    categories,
    status,
    regionId,
    sido,
    page,
    sort,
    setKeyword,
    toggleSourceType,
    toggleCategory,
    setStatus,
    setRegion,
    setSort,
    setPage,
    resetFilters,
  }
}
