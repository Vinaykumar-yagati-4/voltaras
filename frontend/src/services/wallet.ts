import api from '@/services/api'

export interface Wallet {
  id: number
  userId: number
  balance: number
  currency: 'INR'
  createdAt: string
  updatedAt: string
}

export async function getMyWallet(): Promise<Wallet> {
  const { data } = await api.get<Wallet>('/api/wallet/me')
  return data
}

/**
 * Local development/test recharge (no payment gateway). When an
 * organizationId is provided the backend also persists a local recharge
 * transaction so recharge history reflects the credit.
 */
export async function topUpWallet(input: {
  amount: number
  organizationId?: number
}): Promise<Wallet> {
  const { data } = await api.post<Wallet>('/api/wallet/top-up', {
    amount: input.amount,
    ...(input.organizationId != null ? { organizationId: input.organizationId } : {}),
  })
  return data
}
