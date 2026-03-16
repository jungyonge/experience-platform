import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { useCampaignDetail } from '@/hooks/useCampaignDetail'
import { Button } from '@/components/ui/button'
import CampaignDetailHeader from '@/components/CampaignDetailHeader'
import CampaignDetailBody from '@/components/CampaignDetailBody'
import DetailSkeleton from '@/components/DetailSkeleton'
import NotFoundState from '@/components/NotFoundState'
import { AlertCircle, ArrowLeft } from 'lucide-react'

export default function CampaignDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const { campaign, isLoading, isNotFound, error, retry } = useCampaignDetail(id)

  const handleBack = () => {
    const from = (location.state as { from?: string })?.from
    if (from) {
      navigate(from)
    } else if (window.history.length > 1) {
      navigate(-1)
    } else {
      navigate('/')
    }
  }

  if (isLoading) return <DetailSkeleton />
  if (isNotFound) return <NotFoundState />

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center">
        <AlertCircle className="h-12 w-12 text-red-400 mx-auto mb-4" />
        <h2 className="text-xl font-bold mb-2">오류가 발생했습니다</h2>
        <p className="text-gray-500 mb-6">{error}</p>
        <Button onClick={retry}>다시 시도</Button>
      </div>
    )
  }

  if (!campaign) return null

  const isRecruiting = campaign.status === 'RECRUITING'
  const ctaText = isRecruiting ? '원본 사이트에서 신청하기' : '원본 사이트 보기'

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 pb-24 md:pb-6">
      {/* Back navigation */}
      <button
        onClick={handleBack}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-6"
      >
        <ArrowLeft className="h-4 w-4" />
        목록으로 돌아가기
      </button>

      {/* Header: thumbnail + meta */}
      <CampaignDetailHeader campaign={campaign} />

      {/* Body: detail info */}
      <CampaignDetailBody campaign={campaign} />

      {/* Notice */}
      <div className="mt-8 p-4 bg-gray-50 rounded-lg text-xs text-gray-500 space-y-1">
        <p>신청은 원본 사이트에서 진행됩니다.</p>
        <p>본 플랫폼은 정보 제공 목적이며, 실제 체험단 운영은 해당 사이트에서 관리합니다.</p>
      </div>

      {/* CTA - Desktop */}
      <div className="mt-6 hidden md:block">
        <a
          href={campaign.originalUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          <Button
            size="lg"
            className="w-full"
            variant={isRecruiting ? 'default' : 'outline'}
          >
            {ctaText}
          </Button>
        </a>
      </div>

      {/* CTA - Mobile sticky */}
      <div className="fixed bottom-0 left-0 right-0 p-4 bg-white border-t md:hidden z-50">
        <a
          href={campaign.originalUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          <Button
            size="lg"
            className="w-full"
            variant={isRecruiting ? 'default' : 'outline'}
          >
            {ctaText}
          </Button>
        </a>
      </div>
    </div>
  )
}
