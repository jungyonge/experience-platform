import { useState, useCallback, useEffect } from 'react'
import {
  executeAllCrawling,
  executeCrawlingBySource,
  fetchCrawlingLogs,
  fetchCrawlingSources,
} from '@/api/adminApi'
import type {
  CrawlingExecuteResponse,
  CrawlingLogItem,
  CrawlingSourceItem,
} from '@/types/admin'
import { useToast } from '@/hooks/useToast'

interface UseAdminCrawlingReturn {
  activeSources: CrawlingSourceItem[]
  isSourcesLoading: boolean
  executionResult: CrawlingExecuteResponse | null
  isExecuting: boolean
  executingSource: string | null
  logs: CrawlingLogItem[]
  isLogsLoading: boolean
  handleExecuteAll: () => Promise<void>
  handleExecuteBySource: (sourceCode: string) => Promise<void>
  loadLogs: (sourceCode?: string, limit?: number) => Promise<void>
}

export function useAdminCrawling(): UseAdminCrawlingReturn {
  const [activeSources, setActiveSources] = useState<CrawlingSourceItem[]>([])
  const [isSourcesLoading, setIsSourcesLoading] = useState(true)
  const [executionResult, setExecutionResult] = useState<CrawlingExecuteResponse | null>(null)
  const [isExecuting, setIsExecuting] = useState(false)
  const [executingSource, setExecutingSource] = useState<string | null>(null)
  const [logs, setLogs] = useState<CrawlingLogItem[]>([])
  const [isLogsLoading, setIsLogsLoading] = useState(false)
  const { addToast } = useToast()

  useEffect(() => {
    async function loadSources() {
      setIsSourcesLoading(true)
      try {
        const data = await fetchCrawlingSources()
        setActiveSources(data.sources.filter((s) => s.active))
      } catch {
        addToast('소스 목록을 불러오는 중 오류가 발생했습니다.', 'error')
      } finally {
        setIsSourcesLoading(false)
      }
    }
    loadSources()
  }, [addToast])

  const loadLogs = useCallback(
    async (sourceCode?: string, limit?: number) => {
      setIsLogsLoading(true)
      try {
        const data = await fetchCrawlingLogs(sourceCode, limit)
        setLogs(data.logs)
      } catch {
        addToast('로그를 불러오는 중 오류가 발생했습니다.', 'error')
      } finally {
        setIsLogsLoading(false)
      }
    },
    [addToast]
  )

  useEffect(() => {
    loadLogs(undefined, 50)
  }, [loadLogs])

  const handleExecuteAll = useCallback(async () => {
    setIsExecuting(true)
    setExecutingSource(null)
    setExecutionResult(null)
    try {
      const result = await executeAllCrawling()
      setExecutionResult(result)
      addToast('전체 크롤링이 완료되었습니다.', 'success')
      await loadLogs(undefined, 50)
    } catch {
      addToast('크롤링 실행에 실패했습니다.', 'error')
    } finally {
      setIsExecuting(false)
    }
  }, [addToast, loadLogs])

  const handleExecuteBySource = useCallback(
    async (sourceCode: string) => {
      setIsExecuting(true)
      setExecutingSource(sourceCode)
      setExecutionResult(null)
      try {
        const result = await executeCrawlingBySource(sourceCode)
        setExecutionResult(result)
        addToast(`${sourceCode} 크롤링이 완료되었습니다.`, 'success')
        await loadLogs(undefined, 50)
      } catch {
        addToast('크롤링 실행에 실패했습니다.', 'error')
      } finally {
        setIsExecuting(false)
        setExecutingSource(null)
      }
    },
    [addToast, loadLogs]
  )

  return {
    activeSources,
    isSourcesLoading,
    executionResult,
    isExecuting,
    executingSource,
    logs,
    isLogsLoading,
    handleExecuteAll,
    handleExecuteBySource,
    loadLogs,
  }
}
