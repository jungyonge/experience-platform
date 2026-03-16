import { useState, useCallback, type ChangeEvent } from 'react'

interface LoginFormValues {
  email: string
  password: string
}

interface LoginFormErrors {
  email: string
  password: string
}

export function useLoginForm() {
  const [values, setValues] = useState<LoginFormValues>({
    email: '',
    password: '',
  })
  const [errors, setErrors] = useState<LoginFormErrors>({
    email: '',
    password: '',
  })
  const [serverError, setServerError] = useState('')

  const validate = useCallback((name: string, value: string): string => {
    switch (name) {
      case 'email':
        if (!value) return '이메일을 입력해주세요.'
        return ''
      case 'password':
        if (!value) return '비밀번호를 입력해주세요.'
        return ''
      default:
        return ''
    }
  }, [])

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      const { name, value } = e.target
      setValues((prev) => ({ ...prev, [name]: value }))
      setErrors((prev) => ({ ...prev, [name]: validate(name, value) }))
      setServerError('')
    },
    [validate]
  )

  const validateAll = useCallback((): boolean => {
    const newErrors: LoginFormErrors = {
      email: validate('email', values.email),
      password: validate('password', values.password),
    }
    setErrors(newErrors)
    return !newErrors.email && !newErrors.password
  }, [values, validate])

  const isValid = values.email !== '' && values.password !== ''

  return {
    values,
    errors,
    serverError,
    isValid,
    handleChange,
    validateAll,
    setServerError,
  }
}
