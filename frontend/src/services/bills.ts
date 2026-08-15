import api from '@/services/api'

export type BillStatus = 'GENERATED' | 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED'
export type BillPaymentStatus = 'UNPAID' | 'PAID' | 'PARTIALLY_PAID' | 'FAILED' | 'REFUNDED'

export interface BillSummary {
  id: number
  authUserId: number
  meterNumber: string
  billingMonth: number
  billingYear: number
  unitsConsumed: number
  totalAmount: number
  amountPaid: number
  outstandingAmount: number
  billStatus: BillStatus
  paymentStatus: BillPaymentStatus
  generatedDate: string
  dueDate: string
}

export interface BillDetail extends BillSummary {
  meterReadingId: number
  previousReading: number
  currentReading: number
  energyCharge: number
  fixedCharge: number
  taxAmount: number
  lateFee: number
  paidAt: string | null
  remarks: string | null
  generatedBy: number
  createdAt: string
  updatedAt: string
}

/** Returns the consumer's full bill history, newest first (plain array). */
export async function getMyBills(): Promise<BillSummary[]> {
  const { data } = await api.get<BillSummary[]>('/api/bills/me')
  return data
}

export async function getMyBillById(billId: number): Promise<BillDetail> {
  const { data } = await api.get<BillDetail>(`/api/bills/me/${billId}`)
  return data
}

/** Returns still-payable bills (UNPAID / PARTIALLY_PAID / FAILED, not CANCELLED). */
export async function getMyOutstandingBills(): Promise<BillSummary[]> {
  const { data } = await api.get<BillSummary[]>('/api/bills/me/outstanding')
  return data
}

// ---------------------------------------------------------------------------
// Admin bill management
// ---------------------------------------------------------------------------

/** Admin: generates a bill for a consumer from a verified meter reading. */
export async function generateBill(input: {
  authUserId: number
  meterReadingId: number
  meterNumber: string
  previousReading: number
  currentReading: number
  billingMonth: number
  billingYear: number
  generatedDate?: string
  dueDate: string
  remarks?: string
}): Promise<BillDetail> {
  const { data } = await api.post<BillDetail>('/api/bills/admin', input)
  return data
}

export function billPeriodLabel(month: number, year: number): string {
  const date = new Date(year, month - 1, 1)
  return date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' })
}
