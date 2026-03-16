import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useWithdraw } from './useWithdraw'
import * as profileApi from '@/api/profileApi'

vi.mock('@/api/profileApi')

describe('useWithdraw', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('모달 열기/닫기', () => {
    const { result } = renderHook(() => useWithdraw())

    expect(result.current.isModalOpen).toBe(false)

    act(() => {
      result.current.openModal()
    })

    expect(result.current.isModalOpen).toBe(true)

    act(() => {
      result.current.closeModal()
    })

    expect(result.current.isModalOpen).toBe(false)
  })

  it('제출 성공', async () => {
    vi.mocked(profileApi.withdrawMember).mockResolvedValue({ message: '탈퇴 완료' })

    const { result } = renderHook(() => useWithdraw())

    act(() => {
      result.current.openModal()
      result.current.setPassword('password1!')
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(true)
  })

  it('제출 실패 - 서버 에러', async () => {
    vi.mocked(profileApi.withdrawMember).mockRejectedValue({
      message: '현재 비밀번호가 일치하지 않습니다.',
    })

    const { result } = renderHook(() => useWithdraw())

    act(() => {
      result.current.openModal()
      result.current.setPassword('wrong!')
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(false)
    expect(result.current.error).toBe('현재 비밀번호가 일치하지 않습니다.')
  })

  it('비밀번호 미입력 시 제출 불가', async () => {
    const { result } = renderHook(() => useWithdraw())

    act(() => {
      result.current.openModal()
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(false)
    expect(result.current.error).toBe('비밀번호를 입력해주세요.')
    expect(profileApi.withdrawMember).not.toHaveBeenCalled()
  })
})
