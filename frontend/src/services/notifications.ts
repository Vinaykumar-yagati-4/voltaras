import api from '@/services/api'

export type NotificationType =
  | 'BILL_GENERATED'
  | 'PAYMENT_SUCCESS'
  | 'RECHARGE_SUCCESS'
  | 'COMPLAINT_STATUS_UPDATED'
  | 'MANUAL'

export type NotificationStatus = 'UNREAD' | 'READ' | 'FAILED'

export interface Notification {
  id: number
  authUserId: number
  title: string
  message: string
  type: NotificationType
  channel: 'IN_APP' | 'EMAIL' | 'SMS'
  status: NotificationStatus
  referenceType: string | null
  referenceId: number | null
  readAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UnreadCount {
  authUserId: number
  unreadCount: number
}

/** All notifications of the authenticated user, newest first (plain array). */
export async function getMyNotifications(): Promise<Notification[]> {
  const { data } = await api.get<Notification[]>('/api/notifications')
  return data
}

export async function getUnreadCount(): Promise<UnreadCount> {
  const { data } = await api.get<UnreadCount>('/api/notifications/count/unread')
  return data
}

export async function markNotificationRead(id: number): Promise<Notification> {
  const { data } = await api.patch<Notification>(`/api/notifications/${id}/read`)
  return data
}

export async function markAllNotificationsRead(): Promise<void> {
  await api.patch('/api/notifications/read-all')
}
