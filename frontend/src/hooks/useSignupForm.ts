import { useState, useCallback, type ChangeEvent } from "react"
import {
  EMAIL_REGEX,
  PASSWORD_REGEX,
  NICKNAME_REGEX,
  VALIDATION_MESSAGES,
} from "@/constants/validation"
import type { ErrorResponse } from "@/types/member"

interface FormValues {
  email: string
  password: string
  passwordConfirm: string
  nickname: string
}

interface FieldErrors {
  email: string
  password: string
  passwordConfirm: string
  nickname: string
}

interface UseSignupFormReturn {
  values: FormValues
  errors: FieldErrors
  serverError: string
  fieldServerErrors: Record<string, string>
  isValid: boolean
  handleChange: (e: ChangeEvent<HTMLInputElement>) => void
  validateAll: () => boolean
  setServerError: (error: string) => void
  handleServerError: (error: ErrorResponse) => void
  resetServerErrors: () => void
}

const initialValues: FormValues = {
  email: "",
  password: "",
  passwordConfirm: "",
  nickname: "",
}

const initialErrors: FieldErrors = {
  email: "",
  password: "",
  passwordConfirm: "",
  nickname: "",
}

function validateField(name: keyof FormValues, value: string, values: FormValues): string {
  switch (name) {
    case "email":
      if (!value) return VALIDATION_MESSAGES.email.required
      if (!EMAIL_REGEX.test(value)) return VALIDATION_MESSAGES.email.invalid
      return ""
    case "password":
      if (!value) return VALIDATION_MESSAGES.password.required
      if (!PASSWORD_REGEX.test(value)) return VALIDATION_MESSAGES.password.invalid
      return ""
    case "passwordConfirm":
      if (!value) return VALIDATION_MESSAGES.passwordConfirm.required
      if (value !== values.password) return VALIDATION_MESSAGES.passwordConfirm.mismatch
      return ""
    case "nickname":
      if (!value) return VALIDATION_MESSAGES.nickname.required
      if (!NICKNAME_REGEX.test(value)) return VALIDATION_MESSAGES.nickname.invalid
      return ""
    default:
      return ""
  }
}

export function useSignupForm(): UseSignupFormReturn {
  const [values, setValues] = useState<FormValues>(initialValues)
  const [errors, setErrors] = useState<FieldErrors>(initialErrors)
  const [serverError, setServerError] = useState("")
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({})

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      const { name, value } = e.target
      const fieldName = name as keyof FormValues

      const newValues = { ...values, [fieldName]: value }
      setValues(newValues)

      const error = validateField(fieldName, value, newValues)
      setErrors((prev) => ({ ...prev, [fieldName]: error }))

      // 비밀번호 변경 시 비밀번호 확인도 재검증
      if (fieldName === "password" && newValues.passwordConfirm) {
        const confirmError = validateField("passwordConfirm", newValues.passwordConfirm, newValues)
        setErrors((prev) => ({ ...prev, passwordConfirm: confirmError }))
      }
    },
    [values]
  )

  const validateAll = useCallback((): boolean => {
    const newErrors: FieldErrors = {
      email: validateField("email", values.email, values),
      password: validateField("password", values.password, values),
      passwordConfirm: validateField("passwordConfirm", values.passwordConfirm, values),
      nickname: validateField("nickname", values.nickname, values),
    }
    setErrors(newErrors)
    return Object.values(newErrors).every((e) => e === "")
  }, [values])

  const handleServerError = useCallback((error: ErrorResponse) => {
    if (error.errors && error.errors.length > 0) {
      const fieldErrs: Record<string, string> = {}
      error.errors.forEach((fe) => {
        fieldErrs[fe.field] = fe.message
      })
      setFieldServerErrors(fieldErrs)
    } else {
      setServerError(error.message)
    }
  }, [])

  const resetServerErrors = useCallback(() => {
    setServerError("")
    setFieldServerErrors({})
  }, [])

  const isValid =
    Object.values(errors).every((e) => e === "") &&
    Object.values(values).every((v) => v !== "")

  return {
    values,
    errors,
    serverError,
    fieldServerErrors,
    isValid,
    handleChange,
    validateAll,
    setServerError,
    handleServerError,
    resetServerErrors,
  }
}
