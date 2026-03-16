import { useState, useCallback, useEffect } from 'react'
import {
  fetchCrawlingSources,
  createCrawlingSource,
  updateCrawlingSource,
  toggleCrawlingSourceActive,
  testCrawlingSource,
} from '@/api/adminApi'
import type {
  CrawlingSourceItem,
  CrawlingSourceCreateRequest,
  CrawlingSourceUpdateRequest,
  CrawlingTestResponse,
} from '@/types/admin'
import { useToast } from '@/hooks/useToast'

interface UseAdminCrawlingSourcesReturn {
  sources: CrawlingSourceItem[]
  availableCrawlerTypes: string[]
  isLoading: boolean
  error: string | null
  testResult: CrawlingTestResponse | null
  isTestLoading: boolean
  isSaving: boolean
  reload: () => void
  handleCreate: (data: CrawlingSourceCreateRequest) => Promise<boolean>
  handleUpdate: (id: number, data: CrawlingSourceUpdateRequest) => Promise<boolean>
  handleToggleActive: (id: number) => Promise<void>
  handleTest: (id: number) => Promise<void>
  clearTestResult: () => void
}

export function useAdminCrawlingSources(): UseAdminCrawlingSourcesReturn {
  const [sources, setSources] = useState<CrawlingSourceItem[]>([])
  const [availableCrawlerTypes, setAvailableCrawlerTypes] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [testResult, setTestResult] = useState<CrawlingTestResponse | null>(null)
  const [isTestLoading, setIsTestLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const { addToast } = useToast()

  const load = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await fetchCrawlingSources()
      setSources(data.sources)
      setAvailableCrawlerTypes(data.availableCrawlerTypes)
    } catch {
      setError('소스 목록을 불러오는 중 오류가 발생했습니다.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleCreate = useCallback(
    async (data: CrawlingSourceCreateRequest): Promise<boolean> => {
      setIsSaving(true)
      try {
        await createCrawlingSource(data)
        addToast('소스가 생성되었습니다.', 'success')
        await load()
        return true
      } catch (err: unknown) {
        const errorObj = err as { message?: string }
        addToast(errorObj?.message || '소스 생성에 실패했습니다.', 'error')
        return false
      } finally {
        setIsSaving(false)
      }
    },
    [load, addToast]
  )

  const handleUpdate = useCallback(
    async (id: number, data: CrawlingSourceUpdateRequest): Promise<boolean> => {
      setIsSaving(true)
      try {
        await updateCrawlingSource(id, data)
        addToast('소스가 수정되었습니다.', 'success')
        await load()
        return true
      } catch (err: unknown) {
        const errorObj = err as { message?: string }
        addToast(errorObj?.message || '소스 수정에 실패했습니다.', 'error')
        return false
      } finally {
        setIsSaving(false)
      }
    },
    [load, addToast]
  )

  const handleToggleActive = useCallback(
    async (id: number) => {
      try {
        await toggleCrawlingSourceActive(id)
        addToast('상태가 변경되었습니다.', 'success')
        await load()
      } catch {
        addToast('상태 변경에 실패했습니다.', 'error')
      }
    },
    [load, addToast]
  )

  const handleTest = useCallback(
    async (id: number) => {
      setIsTestLoading(true)
      setTestResult(null)
      try {
        const result = await testCrawlingSource(id)
        setTestResult(result)
      } catch {
        addToast('테스트 실행에 실패했습니다.', 'error')
      } finally {
        setIsTestLoading(false)
      }
    },
    [addToast]
  )

  const clearTestResult = useCallback(() => {
    setTestResult(null)
  }, [])

  return {
    sources,
    availableCrawlerTypes,
    isLoading,
    error,
    testResult,
    isTestLoading,
    isSaving,
    reload: load,
    handleCreate,
    handleUpdate,
    handleToggleActive,
    handleTest,
    clearTestResult,
  }
}
