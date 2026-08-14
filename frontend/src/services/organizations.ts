import api from '@/services/api'

export type MembershipRole = 'ADMIN' | 'MANAGER' | 'MEMBER'
export type MembershipStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED' | 'REJECTED'

export interface OrganizationMembership {
  id: number
  organizationId: number
  organizationName: string
  authUserId: number
  membershipRole: MembershipRole
  membershipStatus: MembershipStatus
  joinedAt: string
  createdAt: string
  updatedAt: string
}

/** Memberships of the authenticated user (plain array). */
export async function getMyOrganizations(): Promise<OrganizationMembership[]> {
  const { data } = await api.get<OrganizationMembership[]>('/api/organizations/me')
  return data
}
