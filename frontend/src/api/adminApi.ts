import { apiRequest } from '@/api/apiClient'
import type {
  CrawlingSourceListResponse,
  CrawlingSourceCreateRequest,
  CrawlingSourceUpdateRequest,
  CrawlingSourceItem,
  CrawlingTestResponse,
  CrawlingExecuteResponse,
  CrawlingLogListResponse,
} from '@/types/admin'

export async function fetchCrawlingSources(): Promise<CrawlingSourceListResponse> {
  return apiRequest<CrawlingSourceListResponse>('/api/v1/admin/crawling/sources')
}

export async function createCrawlingSource(
  data: CrawlingSourceCreateRequest
): Promise<CrawlingSourceItem> {
  return apiRequest<CrawlingSourceItem>('/api/v1/admin/crawling/sources', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateCrawlingSource(
  id: number,
  data: CrawlingSourceUpdateRequest
): Promise<CrawlingSourceItem> {
  return apiRequest<CrawlingSourceItem>(`/api/v1/admin/crawling/sources/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function toggleCrawlingSourceActive(
  id: number
): Promise<CrawlingSourceItem> {
  return apiRequest<CrawlingSourceItem>(
    `/api/v1/admin/crawling/sources/${id}/toggle-active`,
    { method: 'PATCH' }
  )
}

export async function testCrawlingSource(
  id: number
): Promise<CrawlingTestResponse> {
  return apiRequest<CrawlingTestResponse>(
    `/api/v1/admin/crawling/sources/${id}/test`,
    { method: 'POST' }
  )
}

export async function executeAllCrawling(): Promise<CrawlingExecuteResponse> {
  return apiRequest<CrawlingExecuteResponse>('/api/v1/admin/crawling/execute', {
    method: 'POST',
  })
}

export async function executeCrawlingBySource(
  sourceCode: string
): Promise<CrawlingExecuteResponse> {
  return apiRequest<CrawlingExecuteResponse>(
    `/api/v1/admin/crawling/execute/${sourceCode}`,
    { method: 'POST' }
  )
}

export async function fetchCrawlingLogs(
  sourceCode?: string,
  limit?: number
): Promise<CrawlingLogListResponse> {
  const query = new URLSearchParams()
  if (sourceCode) query.set('sourceCode', sourceCode)
  if (limit) query.set('limit', String(limit))

  const queryString = query.toString()
  const url = `/api/v1/admin/crawling/logs${queryString ? `?${queryString}` : ''}`
  return apiRequest<CrawlingLogListResponse>(url)
}
