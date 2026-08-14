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
