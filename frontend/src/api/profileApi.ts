import { apiRequest } from '@/api/apiClient'
import type {
  MemberProfileResponse,
  ChangeNicknameRequest,
  ChangePasswordRequest,
  WithdrawRequest,
  MessageResponse,
} from '@/types/profile'

export async function fetchProfile(): Promise<MemberProfileResponse> {
  return apiRequest<MemberProfileResponse>('/api/v1/members/me/profile')
}

export async function changeNickname(data: ChangeNicknameRequest): Promise<MemberProfileResponse> {
  return apiRequest<MemberProfileResponse>('/api/v1/members/me/nickname', {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}

export async function changePassword(data: ChangePasswordRequest): Promise<MessageResponse> {
  return apiRequest<MessageResponse>('/api/v1/members/me/password', {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}

export async function withdrawMember(data: WithdrawRequest): Promise<MessageResponse> {
  return apiRequest<MessageResponse>('/api/v1/members/me', {
    method: 'DELETE',
    body: JSON.stringify(data),
  })
}
