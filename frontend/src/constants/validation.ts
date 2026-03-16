export const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*[\d!@#$%^&*])[a-zA-Z\d!@#$%^&*]{8,}$/
export const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,20}$/
export const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

export const VALIDATION_MESSAGES = {
  email: {
    required: "이메일은 필수입니다.",
    invalid: "이메일 형식이 올바르지 않습니다.",
  },
  password: {
    required: "비밀번호는 필수입니다.",
    invalid: "비밀번호는 8자 이상, 영문과 숫자/특수문자를 포함해야 합니다.",
  },
  passwordConfirm: {
    required: "비밀번호 확인은 필수입니다.",
    mismatch: "비밀번호가 일치하지 않습니다.",
  },
  nickname: {
    required: "닉네임은 필수입니다.",
    invalid: "닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다.",
  },
} as const
