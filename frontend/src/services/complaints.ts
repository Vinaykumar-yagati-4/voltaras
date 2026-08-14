import api from '@/services/api'
import type { PageResponse } from '@/types/api'

export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type ComplaintPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export interface ComplaintSummary {
  id: number
  ticketNumber: string
  consumerId?: number
  categoryId?: number
  categoryName?: string
  subject: string
  status: ComplaintStatus
  priority: ComplaintPriority
  assignedTo?: number | null
  createdAt: string
  updatedAt?: string
  [key: string]: unknown
}

export interface ComplaintComment {
  id: number
  commentText: string
  authorId: number
  createdAt: string
  adminComment: boolean
}

export interface StatusHistoryEntry {
  fromStatus: string | null
  toStatus: string
  changedBy: number
  changedAt: string
}

export interface ComplaintDetail {
  id: number
  ticketNumber: string
  consumerId: number
  categoryId: number
  categoryName: string
  subject: string
  description: string | null
  status: ComplaintStatus
  priority: ComplaintPriority
  assignedTo: number | null
  resolvedAt: string | null
  closedAt: string | null
  createdAt: string
  updatedAt: string
  comments: ComplaintComment[]
  statusHistory: StatusHistoryEntry[]
}

export interface ComplaintCategory {
  id: number
  name: string
  description: string | null
}

export interface CreateComplaintInput {
  categoryId: number
  subject: string
  description: string
}

export async function getMyComplaints(
  page = 0,
  size = 10,
  filters?: { status?: ComplaintStatus; priority?: ComplaintPriority; categoryId?: number },
): Promise<PageResponse<ComplaintSummary>> {
  const { data } = await api.get<PageResponse<ComplaintSummary>>('/api/complaints', {
    params: { page, size, ...filters },
  })
  return data
}

export async function getComplaintDetail(complaintId: number): Promise<ComplaintDetail> {
  const { data } = await api.get<ComplaintDetail>(`/api/complaints/${complaintId}`)
  return data
}

export async function getComplaintCategories(): Promise<ComplaintCategory[]> {
  const { data } = await api.get<ComplaintCategory[]>('/api/complaints/categories')
  return data
}

export async function createComplaint(input: CreateComplaintInput): Promise<ComplaintDetail> {
  const { data } = await api.post<ComplaintDetail>('/api/complaints', input)
  return data
}

export async function addComplaintComment(
  complaintId: number,
  commentText: string,
): Promise<ComplaintComment> {
  const { data } = await api.post<ComplaintComment>(`/api/complaints/${complaintId}/comments`, {
    commentText,
  })
  return data
}

export async function updateComplaint(
  complaintId: number,
  input: { subject: string; description: string },
): Promise<ComplaintDetail> {
  const { data } = await api.put<ComplaintDetail>(`/api/complaints/${complaintId}`, input)
  return data
}

export interface AdminComplaintFilters {
  status?: ComplaintStatus
  priority?: ComplaintPriority
  categoryId?: number
}

export async function getAdminComplaints(
  page = 0,
  size = 10,
  filters?: AdminComplaintFilters,
): Promise<PageResponse<ComplaintSummary>> {
  const { data } = await api.get<PageResponse<ComplaintSummary>>('/api/admin/complaints', {
    params: { page, size, ...filters },
  })
  return data
}

export async function getAdminComplaintDetail(complaintId: number): Promise<ComplaintDetail> {
  const { data } = await api.get<ComplaintDetail>(`/api/admin/complaints/${complaintId}`)
  return data
}

/** Move a complaint along the lifecycle (OPEN → IN_PROGRESS → RESOLVED → CLOSED). */
export async function updateComplaintStatus(
  complaintId: number,
  status: ComplaintStatus,
): Promise<{ complaintId: number; ticketNumber: string; previousStatus: ComplaintStatus; currentStatus: ComplaintStatus }> {
  const { data } = await api.patch(`/api/admin/complaints/${complaintId}/status`, { status })
  return data
}

/** Assign a complaint to an admin (only while OPEN or IN_PROGRESS). */
export async function assignComplaint(
  complaintId: number,
  assignedTo: number,
): Promise<ComplaintDetail> {
  const { data } = await api.put<ComplaintDetail>(`/api/admin/complaints/${complaintId}/assign`, {
    assignedTo,
  })
  return data
}

export async function addAdminComplaintComment(
  complaintId: number,
  commentText: string,
): Promise<ComplaintComment> {
  const { data } = await api.post<ComplaintComment>(
    `/api/admin/complaints/${complaintId}/comments`,
    { commentText },
  )
  return data
}

export async function getComplaintStatusCounts(): Promise<Record<ComplaintStatus, number>> {
  const { data } = await api.get<Record<ComplaintStatus, number>>(
    '/api/complaints/internal/count',
  )
  return data
}
