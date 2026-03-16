import type { CampaignDetailItem } from '@/types/campaign'
import KeywordChip from '@/components/KeywordChip'

interface CampaignDetailBodyProps {
  campaign: CampaignDetailItem
}

export default function CampaignDetailBody({ campaign }: CampaignDetailBodyProps) {
  const hasAnyDetail =
    campaign.reward || campaign.mission || campaign.address ||
    campaign.keywords.length > 0 || campaign.detailContent

  if (!hasAnyDetail) return null

  return (
    <div className="mt-8 space-y-6">
      <hr className="border-gray-200" />

      {campaign.reward && (
        <InfoSection title="제공 내역">
          <p className="text-sm text-gray-700">{campaign.reward}</p>
        </InfoSection>
      )}

      {campaign.mission && (
        <InfoSection title="미션">
          <p className="text-sm text-gray-700">{campaign.mission}</p>
        </InfoSection>
      )}

      {campaign.address && (
        <InfoSection title="방문 주소">
          <p className="text-sm text-gray-700">{campaign.address}</p>
        </InfoSection>
      )}

      {campaign.keywords.length > 0 && (
        <InfoSection title="키워드">
          <div className="flex flex-wrap gap-2">
            {campaign.keywords.map((keyword) => (
              <KeywordChip key={keyword} keyword={keyword} />
            ))}
          </div>
        </InfoSection>
      )}

      {campaign.detailContent && (
        <InfoSection title="상세 설명">
          <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-line">
            {campaign.detailContent}
          </p>
        </InfoSection>
      )}
    </div>
  )
}

function InfoSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="text-sm font-semibold text-gray-900 mb-2">{title}</h3>
      {children}
    </div>
  )
}
