import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck, MailOpen } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import {
  getMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/services/notifications'
import { formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

export function NotificationsPage() {
  const queryClient = useQueryClient()

  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: getMyNotifications,
  })

  const readMutation = useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
      void queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
    },
  })

  const readAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
      void queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
    },
  })

  const notifications = notificationsQuery.data ?? []
  const unread = notifications.filter((n) => n.status === 'UNREAD')

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-lg font-bold text-navy-900">Notifications</h1>
            <p className="text-sm text-slate-500">
              {unread.length > 0 ? `${unread.length} unread` : 'All caught up'}
            </p>
          </div>
          {unread.length > 0 && (
            <Button
              variant="secondary"
              size="sm"
              loading={readAllMutation.isPending}
              onClick={() => readAllMutation.mutate()}
            >
              <CheckCheck className="h-4 w-4" aria-hidden="true" />
              Mark all as read
            </Button>
          )}
        </CardHeader>
        <CardBody className="p-0">
          {notificationsQuery.isLoading ? (
            <LoadingState label="Loading notifications…" />
          ) : notificationsQuery.isError ? (
            <ErrorState
              title="Could not load notifications"
              message={notificationsQuery.error.message}
              onRetry={() => notificationsQuery.refetch()}
            />
          ) : notifications.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No notifications yet"
              description="Updates about your bills, payments, and complaints will appear here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {notifications.map((notification) => (
                <li
                  key={notification.id}
                  className={cn(
                    'flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4',
                    notification.status === 'UNREAD' && 'bg-volt-50/40',
                  )}
                >
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-navy-900">{notification.title}</p>
                    <p className="mt-0.5 break-words text-sm text-slate-600">
                      {notification.message}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">{formatDateTime(notification.createdAt)}</p>
                  </div>
                  <div className="flex shrink-0 items-center">
                    {notification.status === 'UNREAD' ? (
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
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
