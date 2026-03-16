import { useState, useEffect, useCallback } from 'react'
import { fetchProfile, changeNickname as changeNicknameApi } from '@/api/profileApi'
import { useAuth } from '@/hooks/useAuth'
import type { MemberProfileResponse } from '@/types/profile'

export function useProfile() {
  const { updateUser } = useAuth()
  const [profile, setProfile] = useState<MemberProfileResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isChangingNickname, setIsChangingNickname] = useState(false)
  const [nicknameError, setNicknameError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const data = await fetchProfile()
        setProfile(data)
      } catch {
        setError('프로필을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  const changeNickname = useCallback(async (newNickname: string) => {
    setIsChangingNickname(true)
    setNicknameError(null)
    try {
      const updated = await changeNicknameApi({ nickname: newNickname })
      setProfile(updated)
      updateUser({ nickname: updated.nickname })
      return true
    } catch (err: unknown) {
      const errorObj = err as { message?: string }
      setNicknameError(errorObj?.message || '닉네임 변경에 실패했습니다.')
      return false
    } finally {
      setIsChangingNickname(false)
    }
  }, [updateUser])

  return { profile, isLoading, error, changeNickname, isChangingNickname, nicknameError, setNicknameError }
}
