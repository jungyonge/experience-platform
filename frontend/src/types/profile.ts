export interface MemberProfileResponse {
  id: number
  email: string
  nickname: string
  status: string
  statusDisplayName: string
  createdAt: string
}

export interface ChangeNicknameRequest {
  nickname: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  newPasswordConfirm: string
}

export interface WithdrawRequest {
  currentPassword: string
}

export interface MessageResponse {
  message: string
}
