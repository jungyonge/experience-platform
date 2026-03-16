import { renderHook, act } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useSignupForm } from './useSignupForm'
import type { ErrorResponse } from '@/types/member'

describe('useSignupForm', () => {
  it('초기값은 모두 빈 문자열이다', () => {
    const { result } = renderHook(() => useSignupForm())

    expect(result.current.values.email).toBe('')
    expect(result.current.values.password).toBe('')
    expect(result.current.values.passwordConfirm).toBe('')
    expect(result.current.values.nickname).toBe('')
    expect(result.current.isValid).toBe(false)
  })

  it('유효한 이메일 입력 시 에러가 없다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'test@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.email).toBe('')
  })

  it('잘못된 이메일 형식이면 에러 메시지가 표시된다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'invalid-email' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.email).toBe('이메일 형식이 올바르지 않습니다.')
  })

  it('비밀번호 정책 위반 시 에러 메시지가 표시된다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'short' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.password).toBe(
      '비밀번호는 8자 이상, 영문과 숫자/특수문자를 포함해야 합니다.'
    )
  })

  it('유효한 비밀번호 입력 시 에러가 없다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.password).toBe('')
  })

  it('비밀번호 불일치 시 에러 메시지가 표시된다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'passwordConfirm', value: 'Different1' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.passwordConfirm).toBe('비밀번호가 일치하지 않습니다.')
  })

  it('비밀번호 일치 시 에러가 없다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'passwordConfirm', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.passwordConfirm).toBe('')
  })

  it('비밀번호 변경 시 비밀번호 확인도 재검증된다', () => {
    const { result } = renderHook(() => useSignupForm())

    // 먼저 일치시킴
    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'passwordConfirm', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    expect(result.current.errors.passwordConfirm).toBe('')

    // 비밀번호를 변경하면 확인 필드도 불일치 에러
    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Changed123' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.passwordConfirm).toBe('비밀번호가 일치하지 않습니다.')
  })

  it('닉네임 정책 위반 시 에러 메시지가 표시된다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'nickname', value: 'a' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.errors.nickname).toBe(
      '닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다.'
    )
  })

  it('모든 필드가 유효하면 isValid가 true이다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'test@example.com' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'password', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'passwordConfirm', value: 'Test12345' },
      } as React.ChangeEvent<HTMLInputElement>)
    })
    act(() => {
      result.current.handleChange({
        target: { name: 'nickname', value: '테스트유저' },
      } as React.ChangeEvent<HTMLInputElement>)
    })

    expect(result.current.isValid).toBe(true)
  })

  it('validateAll이 모든 필드를 검증하고 결과를 반환한다', () => {
    const { result } = renderHook(() => useSignupForm())

    let isValid: boolean
    act(() => {
      isValid = result.current.validateAll()
    })

    expect(isValid!).toBe(false)
    expect(result.current.errors.email).toBe('이메일은 필수입니다.')
    expect(result.current.errors.password).toBe('비밀번호는 필수입니다.')
    expect(result.current.errors.passwordConfirm).toBe('비밀번호 확인은 필수입니다.')
    expect(result.current.errors.nickname).toBe('닉네임은 필수입니다.')
  })

  it('서버 단일 에러를 처리한다', () => {
    const { result } = renderHook(() => useSignupForm())

    const serverError: ErrorResponse = {
      code: 'DUPLICATE_EMAIL',
      message: '이미 사용 중인 이메일입니다.',
      timestamp: '2026-03-14T10:00:00',
      path: '/api/v1/members/signup',
    }

    act(() => {
      result.current.handleServerError(serverError)
    })

    expect(result.current.serverError).toBe('이미 사용 중인 이메일입니다.')
  })

  it('서버 필드별 에러를 처리한다', () => {
    const { result } = renderHook(() => useSignupForm())

    const serverError: ErrorResponse = {
      code: 'VALIDATION_FAILED',
      message: '입력값이 올바르지 않습니다.',
      errors: [
        { field: 'email', message: '이메일 형식이 올바르지 않습니다.' },
        { field: 'nickname', message: '닉네임은 필수입니다.' },
      ],
      timestamp: '2026-03-14T10:00:00',
      path: '/api/v1/members/signup',
    }

    act(() => {
      result.current.handleServerError(serverError)
    })

    expect(result.current.fieldServerErrors['email']).toBe('이메일 형식이 올바르지 않습니다.')
    expect(result.current.fieldServerErrors['nickname']).toBe('닉네임은 필수입니다.')
  })

  it('resetServerErrors로 서버 에러가 초기화된다', () => {
    const { result } = renderHook(() => useSignupForm())

    act(() => {
      result.current.handleServerError({
        code: 'DUPLICATE_EMAIL',
        message: '이미 사용 중인 이메일입니다.',
        timestamp: '',
        path: '',
      })
    })
    expect(result.current.serverError).toBe('이미 사용 중인 이메일입니다.')

    act(() => {
      result.current.resetServerErrors()
    })
    expect(result.current.serverError).toBe('')
    expect(result.current.fieldServerErrors).toEqual({})
  })
})
