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

/** One day of the daily usage series returned by the daily-usage endpoints. */
export interface DailyUsageEntry {
  date: string
  units: number
  estimatedCost: number | null
  previousReading: number | null
  currentReading: number | null
  readingAt: string | null
}

/**
 * Daily electricity usage summary calculated by the backend from the
 * consumer's real recorded meter readings. Cost fields are estimates.
 */
export interface DailyUsage {
  meterNumber: string | null
  usageDate: string
  previousReading: number | null
  latestReading: number | null
  previousReadingAt: string | null
  latestReadingAt: string | null
  unitsConsumedToday: number
  estimatedPerUnitCost: number
  estimatedTodayCost: number
  monthUnitsSoFar: number
  estimatedMonthCost: number
  hasReadings: boolean
  hasReadingToday: boolean
  dailyUsage: DailyUsageEntry[]
}

/** Today's usage + current month + last 7 days, computed by the backend. */
export async function getDailyUsage(): Promise<DailyUsage> {
  const { data } = await api.get<DailyUsage>('/api/meter-readings/me/daily-usage')
  return data
}

/** Usage summary with a configurable look-back window (1–31 days). */
export async function getUsageSummary(days = 7): Promise<DailyUsage> {
  const { data } = await api.get<DailyUsage>('/api/meter-readings/me/usage-summary', {
    params: { days },
  })
  return data
}

// ---------------------------------------------------------------------------
// Admin meter reading management
// ---------------------------------------------------------------------------

/** Admin: records a SUBMITTED reading for a consumer (account preparation). */
export async function createAdminReading(input: {
  authUserId: number
  meterNumber: string
  previousReading: number
  currentReading: number
  readingDate: string
  remarks?: string
}): Promise<MeterReading> {
  const { data } = await api.post<MeterReading>('/api/meter-readings/admin', input)
  return data
}

/** Admin: verifies a meter reading so it can be used for billing. */
export async function verifyAdminReading(readingId: number): Promise<MeterReading> {
  const { data } = await api.patch<MeterReading>(`/api/meter-readings/admin/${readingId}/verify`)
  return data
}
