export interface SignupRequest {
  email: string
  password: string
  passwordConfirm: string
  nickname: string
}

export interface SignupResponse {
  id: number
  email: string
  nickname: string
  status: string
  createdAt: string
}

export interface FieldError {
  field: string
  message: string
}

export interface ErrorResponse {
  code: string
  message: string
  errors?: FieldError[]
  timestamp: string
  path: string
}
