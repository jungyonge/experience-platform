import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'
import { useProfile } from './useProfile'
import * as profileApi from '@/api/profileApi'
import type { MemberProfileResponse } from '@/types/profile'

vi.mock('@/api/profileApi')
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    updateUser: mockUpdateUser,
  }),
}))

const mockUpdateUser = vi.fn()

const mockProfile: MemberProfileResponse = {
  id: 1,
  email: 'test@example.com',
  nickname: '테스트유저',
  status: 'ACTIVE',
  statusDisplayName: '활성',
  createdAt: '2026-03-14T10:00:00',
}

describe('useProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('프로필 로드', async () => {
    vi.mocked(profileApi.fetchProfile).mockResolvedValue(mockProfile)

    const { result } = renderHook(() => useProfile())

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.profile).toEqual(mockProfile)
    expect(result.current.error).toBeNull()
  })

  it('닉네임 변경 성공', async () => {
    const updatedProfile = { ...mockProfile, nickname: '새닉네임' }
    vi.mocked(profileApi.fetchProfile).mockResolvedValue(mockProfile)
    vi.mocked(profileApi.changeNickname).mockResolvedValue(updatedProfile)

    const { result } = renderHook(() => useProfile())

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.changeNickname('새닉네임')
    })

    expect(success).toBe(true)
    expect(result.current.profile?.nickname).toBe('새닉네임')
    expect(mockUpdateUser).toHaveBeenCalledWith({ nickname: '새닉네임' })
  })

  it('닉네임 변경 실패', async () => {
    vi.mocked(profileApi.fetchProfile).mockResolvedValue(mockProfile)
    vi.mocked(profileApi.changeNickname).mockRejectedValue({
      message: '이미 사용 중인 닉네임입니다.',
    })

    const { result } = renderHook(() => useProfile())

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.changeNickname('중복닉네임')
    })

    expect(success).toBe(false)
    expect(result.current.nicknameError).toBe('이미 사용 중인 닉네임입니다.')
  })
})
