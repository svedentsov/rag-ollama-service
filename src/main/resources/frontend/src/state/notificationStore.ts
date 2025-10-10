import { create } from 'zustand';

/**
 * @interface NotificationState
 * @description Определяет структуру состояния для отслеживания уведомлений о новых сообщениях.
 */
interface NotificationState {
  /** @property {Set<string>} notifications - Множество ID сессий, в которых есть непрочитанные обновления. */
  notifications: Set<string>;
  /**
   * @function addNotification
   * @description Добавляет ID сессии в множество уведомлений.
   * @param {string} sessionId - ID сессии для добавления.
   */
  addNotification: (sessionId: string) => void;
  /**
   * @function clearNotification
   * @description Удаляет ID сессии из множества уведомлений.
   * @param {string} sessionId - ID сессии для удаления.
   */
  clearNotification: (sessionId: string) => void;
}

/**
 * Глобальный стор Zustand для управления уведомлениями о новых сообщениях в неактивных чатах.
 * Является единым источником правды о том, в каких чатах есть непрочитанные обновления.
 */
export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: new Set<string>(),
  addNotification: (sessionId) =>
    set((state) => ({
      notifications: new Set(state.notifications).add(sessionId),
    })),
  clearNotification: (sessionId) =>
    set((state) => {
      const newNotifications = new Set(state.notifications);
      newNotifications.delete(sessionId);
      return { notifications: newNotifications };
    }),
}));
