import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useLoginForm } from './useLoginForm'

describe('useLoginForm', () => {
  it('초기 상태는 빈 값', () => {
    const { result } = renderHook(() => useLoginForm())

    expect(result.current.values.email).toBe('')
    expect(result.current.values.password).toBe('')
    expect(result.current.errors.email).toBe('')
    expect(result.current.errors.password).toBe('')
    expect(result.current.serverError).toBe('')
    expect(result.current.isValid).toBe(false)
  })

  it('이메일 입력 시 값 업데이트', () => {
    const { result } = renderHook(() => useLoginForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'user@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.values.email).toBe('user@example.com')
    expect(result.current.errors.email).toBe('')
  })

  it('비밀번호 입력 시 값 업데이트', () => {
    const { result } = renderHook(() => useLoginForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'mypassword' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.values.password).toBe('mypassword')
    expect(result.current.errors.password).toBe('')
  })

  it('이메일과 비밀번호 모두 입력 시 isValid true', () => {
    const { result } = renderHook(() => useLoginForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'user@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'password123' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.isValid).toBe(true)
  })

  it('validateAll - 빈 값 제출 시 에러 반환', () => {
    const { result } = renderHook(() => useLoginForm())

    let isValid: boolean
    act(() => {
      isValid = result.current.validateAll()
    })

    expect(isValid!).toBe(false)
    expect(result.current.errors.email).toBe('이메일을 입력해주세요.')
    expect(result.current.errors.password).toBe('비밀번호를 입력해주세요.')
  })

  it('validateAll - 값이 있으면 통과', () => {
    const { result } = renderHook(() => useLoginForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'user@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'password123' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    let isValid: boolean
    act(() => {
      isValid = result.current.validateAll()
    })

    expect(isValid!).toBe(true)
    expect(result.current.errors.email).toBe('')
    expect(result.current.errors.password).toBe('')
  })

  it('서버 에러 설정 및 입력 시 초기화', () => {
    const { result } = renderHook(() => useLoginForm())

    act(() => {
      result.current.setServerError('이메일 또는 비밀번호가 올바르지 않습니다.')
    })

    expect(result.current.serverError).toBe('이메일 또는 비밀번호가 올바르지 않습니다.')

    // 입력하면 서버 에러 초기화
    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'new@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.serverError).toBe('')
  })
})
