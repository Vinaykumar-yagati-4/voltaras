import api from '@/services/api'

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export interface UserProfile {
  id: number
  authUserId: number
  fullName: string
  phone: string | null
  address: string | null
  city: string | null
  state: string | null
  country: string | null
  postalCode: string | null
  profileImage: string | null
  dateOfBirth: string | null
  gender: Gender | null
  createdAt: string
  updatedAt: string
}

export interface ProfileInput {
  fullName: string
  phone?: string | null
  address?: string | null
  city?: string | null
  state?: string | null
  country?: string | null
  postalCode?: string | null
  profileImage?: string | null
  dateOfBirth?: string | null
  gender?: Gender | null
}

/** 404 is the "no profile yet" state — surfaced separately from other errors. */
export async function getMyProfile(): Promise<UserProfile> {
  const { data } = await api.get<UserProfile>('/api/users/profile')
  return data
}

export async function createMyProfile(input: ProfileInput): Promise<UserProfile> {
  const { data } = await api.post<UserProfile>('/api/users/profile', input)
  return data
}

export async function updateMyProfile(input: ProfileInput): Promise<UserProfile> {
  const { data } = await api.put<UserProfile>('/api/users/profile', input)
  return data
}
