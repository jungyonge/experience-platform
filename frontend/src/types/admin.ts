export interface CrawlingSourceItem {
  id: number
  code: string
  name: string
  baseUrl: string
  listUrlPattern: string | null
  description: string | null
  crawlerType: string
  active: boolean
  displayOrder: number
  campaignCount: number
  lastCrawledAt: string | null
  createdAt: string
}

export interface CrawlingSourceListResponse {
  sources: CrawlingSourceItem[]
  availableCrawlerTypes: string[]
}

export interface CrawlingSourceCreateRequest {
  code: string
  name: string
  baseUrl: string
  listUrlPattern?: string
  description?: string
  crawlerType: string
  displayOrder: number
}

export interface CrawlingSourceUpdateRequest {
  name: string
  baseUrl: string
  listUrlPattern?: string
  description?: string
  crawlerType: string
  displayOrder: number
}

export interface CrawlingTestItem {
  originalId: string
  title: string
  originalUrl: string
  thumbnailUrl: string | null
  category: string | null
}

export interface CrawlingTestResponse {
  sourceCode: string
  sourceName: string
  crawlerType: string
  success: boolean
  totalCount: number
  errorMessage: string | null
  items: CrawlingTestItem[]
}

export interface CrawlingResult {
  sourceType: string
  sourceDisplayName: string
  status: string
  totalCrawled: number
  newCount: number
  updatedCount: number
  failedCount: number
  errorMessage: string | null
  durationMs: number
}

export interface CrawlingExecuteResponse {
  results: CrawlingResult[]
  totalDurationMs: number
  executedAt: string
}

export interface CrawlingLogItem {
  id: number
  sourceType: string
  sourceDisplayName: string
  status: string
  totalCrawled: number
  newCount: number
  updatedCount: number
  failedCount: number
  errorMessage: string | null
  durationMs: number
  executedAt: string
}

export interface CrawlingLogListResponse {
  logs: CrawlingLogItem[]
}
