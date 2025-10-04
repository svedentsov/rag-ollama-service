import { create } from 'zustand';

/**
 * @interface SessionState
 * @description Определяет структуру состояния для отслеживания активной сессии.
 */
interface SessionState {
  /** @property {string | null} currentSessionId - ID активной в данный момент сессии чата. */
  currentSessionId: string | null;
  /**
   * @function setCurrentSessionId
   * @description Действие для установки ID активной сессии.
   * @param {string | null} sessionId - ID сессии или null, если активной сессии нет.
   */
  setCurrentSessionId: (sessionId: string | null) => void;
}

/**
 * Глобальный стор Zustand для хранения ID текущей активной сессии.
 * Является единым источником правды для всего приложения о том, какой чат открыт.
 */
export const useSessionStore = create<SessionState>((set) => ({
  currentSessionId: null,
  setCurrentSessionId: (sessionId) => set({ currentSessionId: sessionId }),
}));
