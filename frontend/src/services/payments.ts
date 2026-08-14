import api from '@/services/api'
import type { PageResponse } from '@/types/api'

export type PaymentStatus = 'CREATED' | 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED'
export type TransactionType = 'RECHARGE' | 'BILL_PAYMENT' | 'REFUND'
export type PaymentMethod = 'UPI' | 'CARD' | 'WALLET'

export interface Payment {
  id: number
  paymentReference: string
  transactionType: TransactionType
  billId: number | null
  organizationId: number
  userId: number
  amount: number
  currency: 'INR'
  paymentMethod: PaymentMethod
  status: PaymentStatus
  provider: string | null
  providerTransactionId: string | null
  failureCode: string | null
  failureReason: string | null
  createdAt: string
  updatedAt: string
  paidAt: string | null
}

export interface RechargeTransaction {
  id: number
  rechargeReference: string
  orderId: string
  amount: number
  currency: 'INR'
  paymentMethod: PaymentMethod
  status: PaymentStatus
  provider: 'RAZORPAY'
  providerTransactionId: string | null
  failureCode: string | null
  failureReason: string | null
  createdAt: string
  paidAt: string | null
}

export async function getMyPayments(page = 0, size = 10): Promise<PageResponse<Payment>> {
  const { data } = await api.get<PageResponse<Payment>>('/api/payments', {
    params: { page, size },
  })
  return data
}

export async function getPaymentById(paymentId: number): Promise<Payment> {
  const { data } = await api.get<Payment>(`/api/payments/${paymentId}`)
  return data
}

export async function getMyRecharges(): Promise<RechargeTransaction[]> {
  const { data } = await api.get<RechargeTransaction[]>('/api/recharges/me')
  return data
}

/**
 * Pay a bill from the wallet. Requires an `Idempotency-Key` header:
 * replaying the same key with the same payload returns the original payment.
 */
export async function payBillFromWallet(
  billId: number,
  input: { amount: number; currency: 'INR'; organizationId: number },
  idempotencyKey: string,
): Promise<Payment> {
  const { data } = await api.post<Payment>(`/api/bills/${billId}/payments`, input, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return data
}
