import { Button } from '@/components/ui/button'

interface AccountDangerSectionProps {
  onWithdraw: () => void
}

export default function AccountDangerSection({ onWithdraw }: AccountDangerSectionProps) {
  return (
    <div className="border border-red-200 rounded-lg p-6 bg-red-50/30">
      <h2 className="text-lg font-semibold text-red-600 mb-2">계정 관리</h2>
      <div className="space-y-1 mb-4">
        <p className="text-sm text-gray-600">
          탈퇴 시 계정이 비활성화되며 로그인할 수 없습니다.
        </p>
        <p className="text-sm text-gray-600">
          탈퇴 후에는 복구할 수 없습니다.
        </p>
      </div>
      <Button
        variant="outline"
        className="border-red-300 text-red-600 hover:bg-red-50"
        onClick={onWithdraw}
      >
        회원 탈퇴
      </Button>
    </div>
  )
}
