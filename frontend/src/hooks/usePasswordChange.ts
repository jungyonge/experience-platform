import { useState, useCallback } from 'react'
import { changePassword as changePasswordApi } from '@/api/profileApi'
import { PASSWORD_REGEX, VALIDATION_MESSAGES } from '@/constants/validation'

interface PasswordForm {
  currentPassword: string
  newPassword: string
  newPasswordConfirm: string
}

const initialForm: PasswordForm = {
  currentPassword: '',
  newPassword: '',
  newPasswordConfirm: '',
}

export function usePasswordChange() {
  const [form, setForm] = useState<PasswordForm>(initialForm)
  const [errors, setErrors] = useState<Partial<Record<keyof PasswordForm, string>>>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = useCallback((field: keyof PasswordForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
    setServerError(null)
  }, [])

  const validate = useCallback((): boolean => {
    const newErrors: Partial<Record<keyof PasswordForm, string>> = {}

    if (!form.currentPassword) {
      newErrors.currentPassword = '현재 비밀번호는 필수입니다.'
    }
    if (!form.newPassword) {
      newErrors.newPassword = VALIDATION_MESSAGES.password.required
    } else if (!PASSWORD_REGEX.test(form.newPassword)) {
      newErrors.newPassword = VALIDATION_MESSAGES.password.invalid
    }
    if (!form.newPasswordConfirm) {
      newErrors.newPasswordConfirm = VALIDATION_MESSAGES.passwordConfirm.required
    } else if (form.newPassword !== form.newPasswordConfirm) {
      newErrors.newPasswordConfirm = VALIDATION_MESSAGES.passwordConfirm.mismatch
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [form])

  const submit = useCallback(async (): Promise<boolean> => {
    if (!validate()) return false

    setIsSubmitting(true)
    setServerError(null)
    try {
      await changePasswordApi({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
        newPasswordConfirm: form.newPasswordConfirm,
      })
      setForm(initialForm)
      return true
    } catch (err: unknown) {
      const errorObj = err as { message?: string }
      setServerError(errorObj?.message || '비밀번호 변경에 실패했습니다.')
      return false
    } finally {
      setIsSubmitting(false)
    }
  }, [form, validate])

  const reset = useCallback(() => {
    setForm(initialForm)
    setErrors({})
    setServerError(null)
  }, [])

  return { form, errors, serverError, isSubmitting, handleChange, submit, reset }
}
