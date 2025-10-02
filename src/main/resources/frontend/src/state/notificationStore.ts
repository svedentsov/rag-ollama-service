import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

const NOTIFICATIONS_QUERY_KEY = ['notifications'];

/**
 * Создает и управляет состоянием уведомлений о новых сообщениях.
 * Хранит Set из sessionId, в которых есть непрочитанные обновления.
 */
function useNotificationState() {
  const queryClient = useQueryClient();

  // Инициализируем кэш, если его еще нет
  queryClient.setQueryData(NOTIFICATIONS_QUERY_KEY, (oldData: Set<string> | undefined) => oldData ?? new Set<string>());

  const { data: notifications } = useQuery({
    queryKey: NOTIFICATIONS_QUERY_KEY,
    queryFn: () => queryClient.getQueryData<Set<string>>(NOTIFICATIONS_QUERY_KEY)!,
    staleTime: Infinity, // Эти данные управляются только вручную
  });

  const addMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      queryClient.setQueryData(NOTIFICATIONS_QUERY_KEY, (old: Set<string> | undefined) => {
        const newSet = new Set(old);
        newSet.add(sessionId);
        return newSet;
      });
    },
  });

  const clearMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      queryClient.setQueryData(NOTIFICATIONS_QUERY_KEY, (old: Set<string> | undefined) => {
        const newSet = new Set(old);
        newSet.delete(sessionId);
        return newSet;
      });
    },
  });

  return {
    notifications: notifications ?? new Set<string>(),
    addNotification: addMutation.mutate,
    clearNotification: clearMutation.mutate,
  };
}

// Экспортируем хуки для использования в компонентах
export const useNotifications = () => {
  const { notifications } = useNotificationState();
  return { notifications };
};

export const useAddNotification = () => {
  const { addNotification } = useNotificationState();
  return addNotification;
};

export const useClearNotification = () => {
  const { clearNotification } = useNotificationState();
  return clearNotification;
};
