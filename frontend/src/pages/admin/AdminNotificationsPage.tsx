import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck, MailOpen } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { AdminPageHeader } from '@/pages/admin/AdminPageHeader'
import {
  getMyNotifications,
  getUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/services/notifications'
import { formatDateTime, formatEnumLabel } from '@/utils/format'
import { cn } from '@/utils/cn'

export function AdminNotificationsPage() {
  const queryClient = useQueryClient()

  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: getMyNotifications,
  })

  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    void queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
  }

  const readMutation = useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: invalidate,
  })

  const readAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: invalidate,
  })

  const notifications = notificationsQuery.data ?? []
  const unread = notifications.filter((n) => n.status === 'UNREAD')
  const unreadCount = unreadQuery.data?.unreadCount ?? 0

  return (
    <div className="space-y-5">
      <AdminPageHeader
        title="Notifications"
        description="Alerts about complaints, payments and platform activity."
        actions={
          <span className="inline-flex h-9 items-center rounded-md bg-navy-50 px-3 text-sm font-semibold text-navy-800 ring-1 ring-inset ring-navy-100">
            {unreadCount > 0 ? `${unreadCount} unread` : 'All caught up'}
          </span>
        }
      />

      <Card>
        {unread.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3">
            <p className="text-sm text-slate-600">
              You have{' '}
              <span className="font-semibold text-navy-900">{unread.length}</span> unread{' '}
              {unread.length === 1 ? 'notification' : 'notifications'}.
            </p>
            <Button
              variant="secondary"
              size="sm"
              loading={readAllMutation.isPending}
              onClick={() => readAllMutation.mutate()}
            >
              <CheckCheck className="h-4 w-4" aria-hidden="true" />
              Mark all as read
            </Button>
          </div>
        )}
        <CardBody className="p-0">
          {notificationsQuery.isLoading ? (
            <LoadingState label="Loading notifications…" />
          ) : notificationsQuery.isError ? (
            <ErrorState
              title="Could not load notifications"
              message={notificationsQuery.error?.message}
              onRetry={() => notificationsQuery.refetch()}
            />
          ) : notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-volt-50 to-volt-100 ring-1 ring-inset ring-volt-200">
                <Bell className="h-6 w-6 text-volt-600" aria-hidden="true" />
              </div>
              <p className="text-sm font-semibold text-navy-900">You&apos;re all caught up</p>
              <p className="max-w-sm text-sm text-slate-500">
                New alerts about complaints and platform activity will appear here.
              </p>
            </div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {notifications.map((notification) => {
                const isUnread = notification.status === 'UNREAD'
                return (
                  <li
                    key={notification.id}
                    className={cn(
                      'flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4',
                      isUnread && 'bg-volt-50/40',
                    )}
                  >
                    <div className="flex min-w-0 items-start gap-3">
                      <span
                        className={cn(
                          'mt-1 h-2.5 w-2.5 shrink-0 rounded-full',
                          isUnread ? 'bg-volt-500' : 'bg-slate-300',
                        )}
                        aria-hidden="true"
                      />
                      <div className="min-w-0">
                        <p className="break-words text-sm font-semibold text-navy-900">
                          {notification.title}
                        </p>
                        <p className="mt-0.5 break-words text-sm text-slate-600">
                          {notification.message}
                        </p>
                        <p className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                          <Badge tone={isUnread ? 'blue' : 'slate'}>
                            {formatEnumLabel(notification.type)}
                          </Badge>
                          <span>{formatDateTime(notification.createdAt)}</span>
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center pl-5 sm:pl-0">
                      {isUnread ? (
                        <Button
                          variant="secondary"
                          size="sm"
                          loading={readMutation.isPending}
                          onClick={() => readMutation.mutate(notification.id)}
                        >
                          <MailOpen className="h-4 w-4" aria-hidden="true" />
                          Mark read
                        </Button>
                      ) : (
                        <span className="text-xs font-medium text-emerald-600">Read</span>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
