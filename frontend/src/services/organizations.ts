import api from '@/services/api'
import type { PageResponse } from '@/types/api'

export type MembershipRole = 'ADMIN' | 'MANAGER' | 'MEMBER'
export type MembershipStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED' | 'REJECTED'

export type OrganizationType = 'HOSTEL' | 'INSTITUTION' | 'APARTMENT' | 'COMMERCIAL'
export type OrganizationStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
export type StructureStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
export type UnitStatus = 'AVAILABLE' | 'OCCUPIED' | 'INACTIVE' | 'MAINTENANCE'
export type UnitType = 'ROOM' | 'FLAT' | 'CLASSROOM' | 'LAB' | 'OFFICE' | 'SHOP' | 'OTHER'

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

export interface Organization {
  id: number
  name: string
  organizationCode: string
  organizationType: OrganizationType
  description: string | null
  email: string | null
  phone: string | null
  addressLine1: string | null
  addressLine2: string | null
  city: string | null
  state: string | null
  country: string | null
  postalCode: string | null
  createdByAuthUserId: number
  status: OrganizationStatus
  createdAt: string
  updatedAt: string
}

export interface Building {
  id: number
  organizationId: number
  name: string
  code: string
  description: string | null
  address: string | null
  status: StructureStatus
  createdAt: string
  updatedAt: string
}

export interface Block {
  id: number
  buildingId: number
  buildingName: string
  name: string
  code: string
  description: string | null
  status: StructureStatus
  createdAt: string
  updatedAt: string
}

export interface Floor {
  id: number
  blockId: number
  blockName: string
  floorNumber: number
  name: string | null
  description: string | null
  status: StructureStatus
  createdAt: string
  updatedAt: string
}

export interface Unit {
  id: number
  floorId: number
  floorNumber: number
  unitNumber: string
  unitName: string | null
  unitType: UnitType
  capacity: number | null
  status: UnitStatus
  description: string | null
  createdAt: string
  updatedAt: string
}

/** Memberships of the authenticated user (plain array). */
export async function getMyOrganizations(): Promise<OrganizationMembership[]> {
  const { data } = await api.get<OrganizationMembership[]>('/api/organizations/me')
  return data
}

// ---------------------------------------------------------------------------
// Admin organization management
// ---------------------------------------------------------------------------

export interface AdminOrganizationFilters {
  status?: OrganizationStatus
  type?: OrganizationType
}

export async function getAdminOrganizations(
  page = 0,
  size = 10,
  filters?: AdminOrganizationFilters,
): Promise<PageResponse<Organization>> {
  const { data } = await api.get<PageResponse<Organization>>('/api/admin/organizations', {
    params: { page, size, ...filters },
  })
  return data
}

export async function getAdminOrganization(organizationId: number): Promise<Organization> {
  const { data } = await api.get<Organization>(`/api/admin/organizations/${organizationId}`)
  return data
}

export async function suspendOrganization(organizationId: number): Promise<Organization> {
  const { data } = await api.patch<Organization>(
    `/api/admin/organizations/${organizationId}/suspend`,
  )
  return data
}

export async function activateOrganization(organizationId: number): Promise<Organization> {
  const { data } = await api.patch<Organization>(
    `/api/admin/organizations/${organizationId}/activate`,
  )
  return data
}

export interface UpdateOrganizationInput {
  name: string
  organizationType: OrganizationType
  description?: string
  email?: string
  phone?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
}

export async function updateOrganization(
  organizationId: number,
  input: UpdateOrganizationInput,
): Promise<Organization> {
  const { data } = await api.put<Organization>(`/api/organizations/${organizationId}`, input)
  return data
}

// ---------------------------------------------------------------------------
// Structure hierarchy (buildings → blocks → floors → units)
// ---------------------------------------------------------------------------

export async function getOrganizationBuildings(organizationId: number): Promise<Building[]> {
  const { data } = await api.get<Building[]>(`/api/organizations/${organizationId}/buildings`)
  return data
}

export interface CreateBuildingInput {
  name: string
  code: string
  description?: string
  address?: string
}

export async function createBuilding(
  organizationId: number,
  input: CreateBuildingInput,
): Promise<Building> {
  const { data } = await api.post<Building>(
    `/api/organizations/${organizationId}/buildings`,
    input,
  )
  return data
}

export interface UpdateBuildingInput {
  name: string
  description?: string
  address?: string
}

export async function updateBuilding(buildingId: number, input: UpdateBuildingInput): Promise<Building> {
  const { data } = await api.put<Building>(`/api/buildings/${buildingId}`, input)
  return data
}

export async function deleteBuilding(buildingId: number): Promise<void> {
  await api.delete(`/api/buildings/${buildingId}`)
}

export async function getBuildingBlocks(buildingId: number): Promise<Block[]> {
  const { data } = await api.get<Block[]>(`/api/buildings/${buildingId}/blocks`)
  return data
}

export interface CreateBlockInput {
  name: string
  code: string
  description?: string
}

export async function createBlock(buildingId: number, input: CreateBlockInput): Promise<Block> {
  const { data } = await api.post<Block>(`/api/buildings/${buildingId}/blocks`, input)
  return data
}

export interface UpdateBlockInput {
  name: string
  description?: string
}

export async function updateBlock(blockId: number, input: UpdateBlockInput): Promise<Block> {
  const { data } = await api.put<Block>(`/api/blocks/${blockId}`, input)
  return data
}

export async function deleteBlock(blockId: number): Promise<void> {
  await api.delete(`/api/blocks/${blockId}`)
}

export async function getBlockFloors(blockId: number): Promise<Floor[]> {
  const { data } = await api.get<Floor[]>(`/api/blocks/${blockId}/floors`)
  return data
}

export interface CreateFloorInput {
  floorNumber: number
  name?: string
  description?: string
}

export async function createFloor(blockId: number, input: CreateFloorInput): Promise<Floor> {
  const { data } = await api.post<Floor>(`/api/blocks/${blockId}/floors`, input)
  return data
}

export interface UpdateFloorInput {
  floorNumber: number
  name?: string
  description?: string
}

export async function updateFloor(floorId: number, input: UpdateFloorInput): Promise<Floor> {
  const { data } = await api.put<Floor>(`/api/floors/${floorId}`, input)
  return data
}

export async function deleteFloor(floorId: number): Promise<void> {
  await api.delete(`/api/floors/${floorId}`)
}

export async function getFloorUnits(floorId: number): Promise<Unit[]> {
  const { data } = await api.get<Unit[]>(`/api/floors/${floorId}/units`)
  return data
}

export interface CreateUnitInput {
  unitNumber: string
  unitName?: string
  unitType: UnitType
  capacity?: number
  description?: string
}

export async function createUnit(floorId: number, input: CreateUnitInput): Promise<Unit> {
  const { data } = await api.post<Unit>(`/api/floors/${floorId}/units`, input)
  return data
}

export interface UpdateUnitInput {
  unitName?: string
  unitType: UnitType
  capacity?: number
  description?: string
}

export async function updateUnit(unitId: number, input: UpdateUnitInput): Promise<Unit> {
  const { data } = await api.put<Unit>(`/api/units/${unitId}`, input)
  return data
}

export async function updateUnitStatus(unitId: number, status: UnitStatus): Promise<Unit> {
  const { data } = await api.patch<Unit>(`/api/units/${unitId}/status`, { status })
  return data
}

export async function deleteUnit(unitId: number): Promise<void> {
  await api.delete(`/api/units/${unitId}`)
}

// ---------------------------------------------------------------------------
// Memberships (read-only view for the admin portal)
// ---------------------------------------------------------------------------

export async function getOrganizationMembers(
  organizationId: number,
  page = 0,
  size = 50,
): Promise<PageResponse<OrganizationMembership>> {
  const { data } = await api.get<PageResponse<OrganizationMembership>>(
    `/api/organizations/${organizationId}/members`,
    { params: { page, size } },
  )
  return data
}
