import { type FormEvent, useState } from "react"
import { Link } from "react-router-dom"
import { useSignupForm } from "@/hooks/useSignupForm"
import { signup } from "@/api/memberApi"
import type { ErrorResponse } from "@/types/member"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { AlertCircle, CheckCircle2 } from "lucide-react"

export default function SignupPage() {
  const {
    values,
    errors,
    serverError,
    fieldServerErrors,
    isValid,
    handleChange,
    validateAll,
    handleServerError,
    resetServerErrors,
  } = useSignupForm()

  const [isLoading, setIsLoading] = useState(false)
  const [isSuccess, setIsSuccess] = useState(false)

  const getFieldError = (field: string) => {
    return errors[field as keyof typeof errors] || fieldServerErrors[field] || ""
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    resetServerErrors()

    if (!validateAll()) return

    setIsLoading(true)
    try {
      await signup(values)
      setIsSuccess(true)
    } catch (error) {
      handleServerError(error as ErrorResponse)
    } finally {
      setIsLoading(false)
    }
  }

  if (isSuccess) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6">
            <div className="flex flex-col items-center gap-4 py-8">
              <CheckCircle2 className="h-16 w-16 text-green-500" />
              <h2 className="text-2xl font-bold">회원가입 완료</h2>
              <p className="text-muted-foreground text-center">
                환영합니다! 회원가입이 성공적으로 완료되었습니다.
              </p>
              <Link to="/login">
                <Button className="mt-2">로그인하러 가기</Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-3xl">회원가입</CardTitle>
          <CardDescription>체험단 통합 플랫폼에 가입하세요</CardDescription>
        </CardHeader>
        <CardContent>
          {serverError && (
            <div className="flex items-center gap-2 rounded-md bg-red-50 border border-red-200 p-3 mb-6 text-sm text-red-700">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <FormField
              label="이메일"
              name="email"
              type="email"
              placeholder="example@email.com"
              value={values.email}
              error={getFieldError("email")}
              onChange={handleChange}
            />

            <FormField
              label="닉네임"
              name="nickname"
              type="text"
              placeholder="닉네임을 입력하세요"
              value={values.nickname}
              error={getFieldError("nickname")}
              onChange={handleChange}
            />

            <FormField
              label="비밀번호"
              name="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={values.password}
              error={getFieldError("password")}
              onChange={handleChange}
            />

            <FormField
              label="비밀번호 확인"
              name="passwordConfirm"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              value={values.passwordConfirm}
              error={getFieldError("passwordConfirm")}
              onChange={handleChange}
            />

            <Button
              type="submit"
              size="lg"
              className="w-full mt-2"
              disabled={!isValid || isLoading}
            >
              {isLoading ? "가입 중..." : "회원가입"}
            </Button>
          </form>

          <p className="text-center text-sm text-muted-foreground mt-6">
            이미 계정이 있으신가요?{" "}
            <Link to="/login" className="text-primary font-medium hover:underline">
              로그인
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

interface FormFieldProps {
  label: string
  name: string
  type: string
  placeholder: string
  value: string
  error: string
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
}

function FormField({ label, name, type, placeholder, value, error, onChange }: FormFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={name}>{label}</Label>
      <Input
        id={name}
        name={name}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        className={error ? "border-red-400 focus:ring-red-400" : ""}
      />
      {error && (
        <p className="text-sm text-red-500 flex items-center gap-1">
          <AlertCircle className="h-3 w-3" />
          {error}
        </p>
      )}
    </div>
  )
}
