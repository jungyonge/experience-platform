import CampaignCard from '@/components/CampaignCard'
import type { CampaignItem } from '@/types/campaign'

interface CampaignGridProps {
  campaigns: CampaignItem[]
}

export default function CampaignGrid({ campaigns }: CampaignGridProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      {campaigns.map((campaign) => (
        <CampaignCard key={campaign.id} campaign={campaign} />
      ))}
    </div>
  )
}
