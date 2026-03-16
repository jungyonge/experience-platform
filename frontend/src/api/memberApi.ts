import { apiRequest } from '@/api/apiClient'
import type { SignupRequest, SignupResponse } from '@/types/member'

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  return apiRequest<SignupResponse>('/api/v1/members/signup', {
    method: 'POST',
    body: JSON.stringify(request),
  }, true)
}
