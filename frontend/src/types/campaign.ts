export interface CampaignItem {
  id: number
  sourceType: string
  sourceDisplayName: string
  title: string
  thumbnailUrl: string | null
  originalUrl: string
  category: string
  categoryDisplayName: string
  status: string
  statusDisplayName: string
  recruitCount: number | null
  currentApplicants: number | null
  applyEndDate: string | null
}

export interface CampaignDetailItem {
  id: number
  sourceType: string
  sourceDisplayName: string
  title: string
  description: string | null
  detailContent: string | null
  thumbnailUrl: string | null
  originalUrl: string
  category: string
  categoryDisplayName: string
  status: string
  statusDisplayName: string
  recruitCount: number | null
  currentApplicants: number | null
  applyStartDate: string | null
  applyEndDate: string | null
  announcementDate: string | null
  reward: string | null
  mission: string | null
  address: string | null
  keywords: string[]
  createdAt: string
  updatedAt: string
}

export interface CampaignListResponse {
  campaigns: CampaignItem[]
  totalCount: number
  totalPages: number
  currentPage: number
  hasNext: boolean
}

export interface FilterOption {
  code: string
  name: string
}

export interface FilterOptionsResponse {
  sourceTypes: FilterOption[]
  categories: FilterOption[]
  statuses: FilterOption[]
  sortOptions: FilterOption[]
  regions: FilterOption[]
}

export interface CampaignSearchParams {
  keyword?: string
  sourceTypes?: string[]
  categories?: string[]
  status?: string
  region?: string
  page: number
  size: number
  sort: string
}
