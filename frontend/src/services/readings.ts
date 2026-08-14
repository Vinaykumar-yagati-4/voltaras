import api from '@/services/api'

export type ReadingStatus = 'SUBMITTED' | 'VERIFIED' | 'REJECTED'

export interface MeterReading {
  id: number
  authUserId: number
  meterNumber: string
  billingMonth: number
  billingYear: number
  previousReading: number
  currentReading: number
  unitsConsumed: number
  readingDate: string
  status: ReadingStatus
  remarks: string | null
  verifiedBy: number | null
  verifiedAt: string | null
  createdAt: string
  updatedAt: string
}

/** Returns the consumer's meter readings (plain array, no pagination). */
export async function getMyReadings(): Promise<MeterReading[]> {
  const { data } = await api.get<MeterReading[]>('/api/meter-readings/me')
  return data
}
