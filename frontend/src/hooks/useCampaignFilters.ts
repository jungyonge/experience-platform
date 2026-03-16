import { useState, useEffect, useRef } from 'react'
import { fetchFilterOptions } from '@/api/campaignApi'
import type { FilterOptionsResponse } from '@/types/campaign'

export function useCampaignFilters() {
  const [filters, setFilters] = useState<FilterOptionsResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const fetched = useRef(false)

  useEffect(() => {
    if (fetched.current) return
    fetched.current = true

    fetchFilterOptions()
      .then(setFilters)
      .catch(() => {})
      .finally(() => setIsLoading(false))
  }, [])

  return { filters, isLoading }
}
