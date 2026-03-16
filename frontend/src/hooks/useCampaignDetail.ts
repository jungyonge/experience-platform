import { useState, useEffect, useCallback } from 'react'
import { fetchCampaignDetail } from '@/api/campaignApi'
import type { CampaignDetailItem } from '@/types/campaign'

interface UseCampaignDetailReturn {
  campaign: CampaignDetailItem | null
  isLoading: boolean
  isNotFound: boolean
  error: string | null
  retry: () => void
}

export function useCampaignDetail(id: string | undefined): UseCampaignDetailReturn {
  const [campaign, setCampaign] = useState<CampaignDetailItem | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isNotFound, setIsNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const numericId = id ? Number(id) : NaN

  const load = useCallback(async () => {
    if (!id || isNaN(numericId)) {
      setIsNotFound(true)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)
    setIsNotFound(false)

    try {
      const data = await fetchCampaignDetail(numericId)
      setCampaign(data)
    } catch (err: unknown) {
      const errorObj = err as { code?: string }
      if (errorObj?.code === 'CAMPAIGN_NOT_FOUND') {
        setIsNotFound(true)
      } else {
        setError('데이터를 불러오는 중 오류가 발생했습니다.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [id, numericId])

  useEffect(() => {
    load()
  }, [load])

  return { campaign, isLoading, isNotFound, error, retry: load }
}
