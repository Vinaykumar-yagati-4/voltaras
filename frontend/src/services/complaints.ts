import api from '@/services/api'

export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type ComplaintPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export interface ComplaintSummary {
  id: number
  ticketNumber: string
  categoryId?: number
  categoryName?: string
  subject: string
  status: ComplaintStatus
  priority: ComplaintPriority
  createdAt: string
  updatedAt?: string
  [key: string]: unknown
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  [key: string]: unknown
}

export async function getMyComplaints(page = 0, size = 5): Promise<PageResponse<ComplaintSummary>> {
  const { data } = await api.get<PageResponse<ComplaintSummary>>('/api/complaints', {
    params: { page, size },
  })
  return data
}

export async function getAdminComplaints(
  page = 0,
  size = 5,
): Promise<PageResponse<ComplaintSummary>> {
  const { data } = await api.get<PageResponse<ComplaintSummary>>('/api/admin/complaints', {
    params: { page, size },
  })
  return data
}

export async function getComplaintStatusCounts(): Promise<Record<ComplaintStatus, number>> {
  const { data } = await api.get<Record<ComplaintStatus, number>>(
    '/api/complaints/internal/count',
  )
  return data
}
