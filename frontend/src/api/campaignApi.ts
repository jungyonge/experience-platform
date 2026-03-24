import { apiRequest } from '@/api/apiClient'
import type { CampaignListResponse, CampaignDetailItem, FilterOptionsResponse, CampaignSearchParams } from '@/types/campaign'

export async function fetchCampaigns(params: CampaignSearchParams): Promise<CampaignListResponse> {
  const query = new URLSearchParams()

  if (params.keyword) query.set('keyword', params.keyword)
  if (params.sourceTypes && params.sourceTypes.length > 0) {
    query.set('sourceTypes', params.sourceTypes.join(','))
  }
  if (params.categories && params.categories.length > 0) {
    query.set('categories', params.categories.join(','))
  }
  if (params.status) query.set('status', params.status)
  if (params.region) query.set('region', params.region)
  query.set('page', String(params.page))
  query.set('size', String(params.size))
  query.set('sort', params.sort)

  return apiRequest<CampaignListResponse>(`/api/v1/campaigns?${query.toString()}`, {}, true)
}

export async function fetchCampaignDetail(id: number): Promise<CampaignDetailItem> {
  return apiRequest<CampaignDetailItem>(`/api/v1/campaigns/${id}`, {}, true)
}

export async function fetchFilterOptions(): Promise<FilterOptionsResponse> {
  return apiRequest<FilterOptionsResponse>('/api/v1/campaigns/filters', {}, true)
}
