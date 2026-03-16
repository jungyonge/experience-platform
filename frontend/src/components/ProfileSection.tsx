import type { MemberProfileResponse } from '@/types/profile'
import NicknameEditor from '@/components/NicknameEditor'
import { formatDate } from '@/utils/dateUtils'
import { Lock } from 'lucide-react'

interface ProfileSectionProps {
  profile: MemberProfileResponse
  onChangeNickname: (nickname: string) => Promise<boolean>
  isChangingNickname: boolean
  nicknameError: string | null
  onClearNicknameError: () => void
}

export default function ProfileSection({
  profile,
  onChangeNickname,
  isChangingNickname,
  nicknameError,
  onClearNicknameError,
}: ProfileSectionProps) {
  return (
    <div className="bg-white border rounded-lg p-6">
      <h2 className="text-lg font-semibold mb-4">프로필 정보</h2>

      <div className="space-y-4">
        {/* 이메일 */}
        <div className="flex items-start gap-4">
          <span className="text-sm text-gray-500 w-20 shrink-0 pt-0.5">이메일</span>
          <div className="flex items-center gap-2 text-sm text-gray-400">
            <Lock className="h-3.5 w-3.5" />
            <span>{profile.email}</span>
          </div>
        </div>

        {/* 닉네임 */}
        <div className="flex items-start gap-4">
          <span className="text-sm text-gray-500 w-20 shrink-0 pt-0.5">닉네임</span>
          <NicknameEditor
            currentNickname={profile.nickname}
            onSave={onChangeNickname}
            isSaving={isChangingNickname}
            serverError={nicknameError}
            onClearError={onClearNicknameError}
          />
        </div>

        {/* 가입일 */}
        <div className="flex items-start gap-4">
          <span className="text-sm text-gray-500 w-20 shrink-0 pt-0.5">가입일</span>
          <span className="text-sm text-gray-900">
            {formatDate(profile.createdAt?.substring(0, 10)) || '-'}
          </span>
        </div>
      </div>
    </div>
  )
}
