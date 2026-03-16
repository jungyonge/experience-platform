import type { CampaignDetailItem } from '@/types/campaign'
import StatusBadge from '@/components/StatusBadge'
import { getDdayText, formatPeriod, formatDate } from '@/utils/dateUtils'

interface CampaignDetailHeaderProps {
  campaign: CampaignDetailItem
}

export default function CampaignDetailHeader({ campaign }: CampaignDetailHeaderProps) {
  const ddayText = campaign.status === 'RECRUITING' ? getDdayText(campaign.applyEndDate) : null
  const period = formatPeriod(campaign.applyStartDate, campaign.applyEndDate)
  const announcement = formatDate(campaign.announcementDate)

  const sourceColorClass =
    campaign.sourceType === 'REVU'
      ? 'bg-blue-100 text-blue-700'
      : campaign.sourceType === 'MBLE'
        ? 'bg-purple-100 text-purple-700'
        : 'bg-green-100 text-green-700'

  return (
    <div className="flex flex-col md:flex-row gap-6">
      {/* Thumbnail */}
      <div className="w-full md:w-[400px] shrink-0">
        <div className="relative aspect-[3/2] bg-gray-100 rounded-lg overflow-hidden">
          <img
            src={campaign.thumbnailUrl || 'https://placehold.co/600x400?text=No+Image'}
            alt={campaign.title}
            className="w-full h-full object-cover"
            onError={(e) => {
              (e.target as HTMLImageElement).src = 'https://placehold.co/600x400?text=No+Image'
            }}
          />
        </div>
      </div>

      {/* Meta info */}
      <div className="flex-1">
        <h1 className="text-xl md:text-2xl font-bold mb-3">{campaign.title}</h1>

        {/* Badges */}
        <div className="flex flex-wrap items-center gap-2 mb-4">
          <span className={`text-xs font-medium px-2 py-0.5 rounded ${sourceColorClass}`}>
            {campaign.sourceDisplayName}
          </span>
          <span className="text-xs font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-600">
            {campaign.categoryDisplayName}
          </span>
          <StatusBadge status={campaign.status} statusDisplayName={campaign.statusDisplayName} />
          {ddayText && (
            <span className={`text-xs font-bold px-2 py-0.5 rounded ${
              ddayText === '마감' ? 'bg-gray-100 text-gray-500' :
              ddayText === 'D-Day' ? 'bg-red-100 text-red-600' :
              'bg-orange-50 text-orange-600'
            }`}>
              {ddayText}
            </span>
          )}
        </div>

        {/* Info list */}
        <div className="space-y-2 text-sm text-gray-600">
          {campaign.recruitCount != null && (
            <div className="flex items-center gap-2">
              <span className="text-gray-400 w-16 shrink-0">모집인원</span>
              <span>{campaign.recruitCount}명</span>
            </div>
          )}
          {period && (
            <div className="flex items-center gap-2">
              <span className="text-gray-400 w-16 shrink-0">신청기간</span>
              <span>{period}</span>
            </div>
          )}
          {announcement && (
            <div className="flex items-center gap-2">
              <span className="text-gray-400 w-16 shrink-0">발표일</span>
              <span>{announcement}</span>
            </div>
          )}
        </div>

        {campaign.description && (
          <p className="mt-4 text-sm text-gray-500 leading-relaxed">
            {campaign.description}
          </p>
        )}
      </div>
    </div>
  )
}
