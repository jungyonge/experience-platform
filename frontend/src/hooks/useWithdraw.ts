import { useState, useCallback } from 'react'
import { withdrawMember } from '@/api/profileApi'

export function useWithdraw() {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const openModal = useCallback(() => {
    setIsModalOpen(true)
    setPassword('')
    setError(null)
  }, [])

  const closeModal = useCallback(() => {
    setIsModalOpen(false)
    setPassword('')
    setError(null)
  }, [])

  const submit = useCallback(async (): Promise<boolean> => {
    if (!password) {
      setError('비밀번호를 입력해주세요.')
      return false
    }

    setIsSubmitting(true)
    setError(null)
    try {
      await withdrawMember({ currentPassword: password })
      return true
    } catch (err: unknown) {
      const errorObj = err as { message?: string }
      setError(errorObj?.message || '회원 탈퇴에 실패했습니다.')
      return false
    } finally {
      setIsSubmitting(false)
    }
  }, [password])

  return { isModalOpen, password, setPassword, isSubmitting, error, openModal, closeModal, submit }
}
