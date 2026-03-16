import { useNavigate } from 'react-router-dom'
import { useProfile } from '@/hooks/useProfile'
import { useWithdraw } from '@/hooks/useWithdraw'
import { useAuth } from '@/hooks/useAuth'
import { useToast } from '@/hooks/useToast'
import ProfileSection from '@/components/ProfileSection'
import PasswordChangeSection from '@/components/PasswordChangeSection'
import AccountDangerSection from '@/components/AccountDangerSection'
import WithdrawModal from '@/components/WithdrawModal'

export default function MyPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const { addToast } = useToast()
  const {
    profile,
    isLoading,
    error,
    changeNickname,
    isChangingNickname,
    nicknameError,
    setNicknameError,
  } = useProfile()

  const withdraw = useWithdraw()

  const handleChangeNickname = async (nickname: string) => {
    const success = await changeNickname(nickname)
    if (success) {
      addToast('닉네임이 변경되었습니다.', 'success')
    }
    return success
  }

  const handlePasswordSuccess = () => {
    addToast('비밀번호가 변경되었습니다.', 'success')
  }

  const handleWithdraw = async () => {
    const success = await withdraw.submit()
    if (success) {
      withdraw.closeModal()
      await logout()
      navigate('/')
      addToast('회원 탈퇴가 완료되었습니다.', 'success')
    }
  }

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10">
        <div className="animate-pulse space-y-6">
          <div className="h-8 w-32 bg-gray-200 rounded" />
          <div className="h-40 bg-gray-200 rounded-lg" />
          <div className="h-16 bg-gray-200 rounded-lg" />
          <div className="h-24 bg-gray-200 rounded-lg" />
        </div>
      </div>
    )
  }

  if (error || !profile) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <p className="text-gray-500">{error || '프로필을 불러올 수 없습니다.'}</p>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <h1 className="text-2xl font-bold">마이페이지</h1>

      <ProfileSection
        profile={profile}
        onChangeNickname={handleChangeNickname}
        isChangingNickname={isChangingNickname}
        nicknameError={nicknameError}
        onClearNicknameError={() => setNicknameError(null)}
      />

      <PasswordChangeSection onSuccess={handlePasswordSuccess} />

      <AccountDangerSection onWithdraw={withdraw.openModal} />

      <WithdrawModal
        isOpen={withdraw.isModalOpen}
        password={withdraw.password}
        onPasswordChange={withdraw.setPassword}
        isSubmitting={withdraw.isSubmitting}
        error={withdraw.error}
        onSubmit={handleWithdraw}
        onClose={withdraw.closeModal}
      />
    </div>
  )
}
