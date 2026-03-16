import { Link, useLocation } from 'react-router-dom'
import type { CampaignItem } from '@/types/campaign'
import StatusBadge from '@/components/StatusBadge'

interface CampaignCardProps {
  campaign: CampaignItem
}

export default function CampaignCard({ campaign }: CampaignCardProps) {
  const location = useLocation()
  const isClosed = campaign.status === 'CLOSED'

  return (
    <Link
      to={`/campaigns/${campaign.id}`}
      state={{ from: location.pathname + location.search }}
      className={`
        block rounded-lg border bg-white overflow-hidden shadow-sm
        transition-all hover:shadow-md hover:-translate-y-0.5
        ${isClosed ? 'opacity-70' : ''}
      `}
    >
      <div className="relative aspect-[3/2] bg-gray-100">
        <img
          src={campaign.thumbnailUrl || 'https://placehold.co/300x200?text=No+Image'}
          alt={campaign.title}
          className="w-full h-full object-cover"
          onError={(e) => {
            (e.target as HTMLImageElement).src = 'https://placehold.co/300x200?text=No+Image'
          }}
        />
        <span
          className={`
            absolute top-2 left-2 px-2 py-0.5 rounded text-xs font-medium
            ${campaign.sourceType === 'REVU' ? 'bg-blue-100 text-blue-700' : ''}
            ${campaign.sourceType === 'MBLE' ? 'bg-purple-100 text-purple-700' : ''}
            ${campaign.sourceType === 'GANGNAM' ? 'bg-green-100 text-green-700' : ''}
          `}
        >
          {campaign.sourceDisplayName}
        </span>
      </div>

      <div className="p-3">
        <h3 className="font-medium text-sm leading-5 line-clamp-2 mb-2">
          {campaign.title}
        </h3>

        <div className="flex items-center gap-2 mb-2">
          <span className="text-xs text-gray-500">{campaign.categoryDisplayName}</span>
          <StatusBadge status={campaign.status} statusDisplayName={campaign.statusDisplayName} />
        </div>

        <div className="flex items-center justify-between text-xs text-gray-400">
          {campaign.applyEndDate && (
            <span>~{campaign.applyEndDate}</span>
          )}
          {campaign.recruitCount != null && (
            <span>{campaign.recruitCount}명</span>
          )}
        </div>
      </div>
    </Link>
  )
}
