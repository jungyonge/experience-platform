import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NICKNAME_REGEX, VALIDATION_MESSAGES } from '@/constants/validation'
import { AlertCircle } from 'lucide-react'

interface NicknameEditorProps {
  currentNickname: string
  onSave: (nickname: string) => Promise<boolean>
  isSaving: boolean
  serverError: string | null
  onClearError: () => void
}

export default function NicknameEditor({
  currentNickname,
  onSave,
  isSaving,
  serverError,
  onClearError,
}: NicknameEditorProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [value, setValue] = useState(currentNickname)
  const [error, setError] = useState<string | null>(null)

  const handleEdit = () => {
    setValue(currentNickname)
    setError(null)
    onClearError()
    setIsEditing(true)
  }

  const handleCancel = () => {
    setIsEditing(false)
    setError(null)
    onClearError()
  }

  const handleSave = async () => {
    if (!value.trim()) {
      setError(VALIDATION_MESSAGES.nickname.required)
      return
    }
    if (!NICKNAME_REGEX.test(value)) {
      setError(VALIDATION_MESSAGES.nickname.invalid)
      return
    }
    const success = await onSave(value)
    if (success) {
      setIsEditing(false)
      setError(null)
    }
  }

  const displayError = error || serverError

  if (!isEditing) {
    return (
      <div className="flex items-center gap-2">
        <span className="text-sm text-gray-900">{currentNickname}</span>
        <button
          onClick={handleEdit}
          className="text-xs text-primary hover:underline"
        >
          수정
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <Input
          value={value}
          onChange={(e) => {
            setValue(e.target.value)
            setError(null)
            onClearError()
          }}
          className="h-8 text-sm max-w-[200px]"
          placeholder="새 닉네임"
        />
        <Button size="sm" onClick={handleSave} disabled={isSaving}>
          {isSaving ? '저장 중...' : '저장'}
        </Button>
        <Button size="sm" variant="outline" onClick={handleCancel} disabled={isSaving}>
          취소
        </Button>
      </div>
      {displayError && (
        <p className="text-xs text-red-500 flex items-center gap-1">
          <AlertCircle className="h-3 w-3" />
          {displayError}
        </p>
      )}
    </div>
  )
}
