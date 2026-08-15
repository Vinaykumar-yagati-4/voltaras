import api from '@/services/api'

export type MeterType = 'SMART' | 'ANALOG' | 'PREPAID'
export type ConnectionType = 'RESIDENTIAL' | 'COMMERCIAL' | 'INDUSTRIAL' | 'AGRICULTURAL'
export type PhaseType = 'SINGLE_PHASE' | 'THREE_PHASE'
export type MeterStatus = 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'REPLACED' | 'REMOVED'

export interface MeterSummary {
  id: number
  meterNumber: string
  authUserId: number | null
  organizationId: number | null
  meterType: MeterType
  connectionType: ConnectionType
  phaseType: PhaseType
  status: MeterStatus
  sanctionedLoadKw: number
  city: string | null
}

export interface Meter extends MeterSummary {
  installationDate: string | null
  addressLine: string | null
  state: string | null
  pincode: string | null
  remarks: string | null
  createdAt: string
  updatedAt: string
}

/** Meters assigned to the authenticated consumer (newest first). */
export async function getMyMeters(): Promise<MeterSummary[]> {
  const { data } = await api.get<MeterSummary[]>('/api/meters')
  return data
}

// ---------------------------------------------------------------------------
// Admin meter management
// ---------------------------------------------------------------------------

export interface CreateMeterInput {
  meterNumber: string
  meterType: MeterType
  connectionType: ConnectionType
  phaseType: PhaseType
  status?: MeterStatus
  sanctionedLoadKw: number
  installationDate?: string
  addressLine?: string
  city?: string
  state?: string
  pincode?: string
  remarks?: string
}

/** Registers a new physical meter (ADMIN). */
export async function createMeter(input: CreateMeterInput): Promise<Meter> {
  const { data } = await api.post<Meter>('/api/meters/admin', input)
  return data
}

/** Assigns a meter to a consumer, optionally linking an organization (ADMIN). */
export async function assignMeter(
  meterId: number,
  input: { authUserId: number; organizationId?: number },
): Promise<Meter> {
  const { data } = await api.patch<Meter>(`/api/meters/admin/${meterId}/assign`, input)
  return data
}

/** Admin: meters assigned to a given user (empty array when none). */
export async function getAdminMetersByUser(authUserId: number): Promise<MeterSummary[]> {
  const { data } = await api.get<MeterSummary[]>('/api/meters/admin', {
    params: { authUserId },
  })
  return data
}
