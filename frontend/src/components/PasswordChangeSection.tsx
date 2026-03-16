import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { usePasswordChange } from '@/hooks/usePasswordChange'
import { AlertCircle, ChevronDown, ChevronUp } from 'lucide-react'

interface PasswordChangeSectionProps {
  onSuccess: () => void
}

export default function PasswordChangeSection({ onSuccess }: PasswordChangeSectionProps) {
  const [isOpen, setIsOpen] = useState(false)
  const { form, errors, serverError, isSubmitting, handleChange, submit, reset } = usePasswordChange()

  const handleSubmit = async () => {
    const success = await submit()
    if (success) {
      onSuccess()
    }
  }

  const handleToggle = () => {
    if (isOpen) {
      reset()
    }
    setIsOpen(!isOpen)
  }

  return (
    <div className="bg-white border rounded-lg">
      <button
        onClick={handleToggle}
        className="w-full flex items-center justify-between p-6"
      >
        <h2 className="text-lg font-semibold">비밀번호 변경</h2>
        {isOpen ? <ChevronUp className="h-5 w-5 text-gray-400" /> : <ChevronDown className="h-5 w-5 text-gray-400" />}
      </button>

      {isOpen && (
        <div className="px-6 pb-6 space-y-4">
          <p className="text-xs text-gray-400">
            비밀번호는 8자 이상, 영문과 숫자/특수문자를 포함해야 합니다.
          </p>

          {serverError && (
            <div className="flex items-center gap-2 rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          <PasswordField
            label="현재 비밀번호"
            value={form.currentPassword}
            error={errors.currentPassword}
            onChange={(v) => handleChange('currentPassword', v)}
          />
          <PasswordField
            label="새 비밀번호"
            value={form.newPassword}
            error={errors.newPassword}
            onChange={(v) => handleChange('newPassword', v)}
          />
          <PasswordField
            label="새 비밀번호 확인"
            value={form.newPasswordConfirm}
            error={errors.newPasswordConfirm}
            onChange={(v) => handleChange('newPasswordConfirm', v)}
          />

          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? '변경 중...' : '비밀번호 변경'}
          </Button>
        </div>
      )}
    </div>
  )
}

function PasswordField({
  label,
  value,
  error,
  onChange,
}: {
  label: string
  value: string
  error?: string
  onChange: (value: string) => void
}) {
  return (
    <div className="space-y-1">
      <label className="text-sm text-gray-600">{label}</label>
      <Input
        type="password"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={error ? 'border-red-400' : ''}
      />
      {error && (
        <p className="text-xs text-red-500 flex items-center gap-1">
          <AlertCircle className="h-3 w-3" />
          {error}
        </p>
      )}
    </div>
  )
}
