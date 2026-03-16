import { useEffect, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { AlertCircle } from 'lucide-react'

interface WithdrawModalProps {
  isOpen: boolean
  password: string
  onPasswordChange: (value: string) => void
  isSubmitting: boolean
  error: string | null
  onSubmit: () => void
  onClose: () => void
}

export default function WithdrawModal({
  isOpen,
  password,
  onPasswordChange,
  isSubmitting,
  error,
  onSubmit,
  onClose,
}: WithdrawModalProps) {
  const overlayRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleEscape)
    }
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-[200] flex items-center justify-center bg-black/50"
      onClick={(e) => {
        if (e.target === overlayRef.current) onClose()
      }}
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4 p-6">
        <h3 className="text-lg font-semibold text-red-600 mb-2">회원 탈퇴</h3>
        <p className="text-sm text-gray-600 mb-4">
          탈퇴 후에는 복구할 수 없습니다. 계속하시려면 현재 비밀번호를 입력해주세요.
        </p>

        {error && (
          <div className="flex items-center gap-2 rounded-md bg-red-50 border border-red-200 p-3 mb-4 text-sm text-red-700">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="mb-4">
          <label className="text-sm text-gray-600 mb-1 block">현재 비밀번호</label>
          <Input
            type="password"
            value={password}
            onChange={(e) => onPasswordChange(e.target.value)}
            placeholder="비밀번호를 입력하세요"
          />
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            취소
          </Button>
          <Button
            onClick={onSubmit}
            disabled={isSubmitting || !password}
            className="bg-red-600 hover:bg-red-700 text-white"
          >
            {isSubmitting ? '처리 중...' : '탈퇴하기'}
          </Button>
        </div>
      </div>
    </div>
  )
}
