import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { usePasswordChange } from './usePasswordChange'
import * as profileApi from '@/api/profileApi'

vi.mock('@/api/profileApi')

describe('usePasswordChange', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('폼 상태 초기값', () => {
    const { result } = renderHook(() => usePasswordChange())

    expect(result.current.form.currentPassword).toBe('')
    expect(result.current.form.newPassword).toBe('')
    expect(result.current.form.newPasswordConfirm).toBe('')
    expect(result.current.isSubmitting).toBe(false)
  })

  it('폼 값 변경', () => {
    const { result } = renderHook(() => usePasswordChange())

    act(() => {
      result.current.handleChange('currentPassword', 'oldPass1!')
    })

    expect(result.current.form.currentPassword).toBe('oldPass1!')
  })

  it('클라이언트 검증 - 비밀번호 정책 위반', async () => {
    const { result } = renderHook(() => usePasswordChange())

    act(() => {
      result.current.handleChange('currentPassword', 'old1!')
      result.current.handleChange('newPassword', 'weak')
      result.current.handleChange('newPasswordConfirm', 'weak')
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(false)
    expect(result.current.errors.newPassword).toBeTruthy()
  })

  it('클라이언트 검증 - 새 비밀번호 확인 불일치', async () => {
    const { result } = renderHook(() => usePasswordChange())

    act(() => {
      result.current.handleChange('currentPassword', 'OldPass1!')
      result.current.handleChange('newPassword', 'NewPass1!')
      result.current.handleChange('newPasswordConfirm', 'Different1!')
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(false)
    expect(result.current.errors.newPasswordConfirm).toBeTruthy()
  })

  it('제출 후 초기화', async () => {
    vi.mocked(profileApi.changePassword).mockResolvedValue({ message: '성공' })

    const { result } = renderHook(() => usePasswordChange())

    act(() => {
      result.current.handleChange('currentPassword', 'OldPass1!')
      result.current.handleChange('newPassword', 'NewPass1!')
      result.current.handleChange('newPasswordConfirm', 'NewPass1!')
    })

    let success: boolean = false
    await act(async () => {
      success = await result.current.submit()
    })

    expect(success).toBe(true)
    expect(result.current.form.currentPassword).toBe('')
    expect(result.current.form.newPassword).toBe('')
    expect(result.current.form.newPasswordConfirm).toBe('')
  })
})
