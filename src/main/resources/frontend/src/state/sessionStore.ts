import { create } from 'zustand';
import { ChatSession } from '../types';

/**
 * @interface SessionState
 * @description Определяет структуру состояния для отслеживания активной сессии и связанных с ней UI-настроек.
 */
interface SessionState {
  /** @property {string | null} currentSessionId - ID активной в данный момент сессии чата. */
  currentSessionId: string | null;
  /**
   * @property {Map<string, Record<string, string>>} sessionBranches - Карта, где ключ - ID сессии,
   * а значение - объект с активными ветками для этой сессии (`{ [parentId]: childId }`).
   */
  sessionBranches: Map<string, Record<string, string>>;
  /**
   * @function setCurrentSessionId
   * @description Устанавливает ID активной сессии.
   * @param {string | null} sessionId - ID сессии или null.
   */
  setCurrentSessionId: (sessionId: string | null) => void;
  /**
   * @function setSessions
   * @description Устанавливает полный список сессий, извлекая из них информацию об активных ветках.
   * @param {ChatSession[]} sessions - Массив сессий, полученный от API.
   */
  setSessions: (sessions: ChatSession[]) => void;
  /**
   * @function setActiveBranch
   * @description Устанавливает активную ветку для конкретной сессии.
   * @param {string} sessionId - ID сессии.
   * @param {string} parentId - ID родительского сообщения.
   * @param {string} childId - ID выбранного дочернего сообщения.
   */
  setActiveBranch: (sessionId: string, parentId: string, childId: string) => void;
  /**
   * @function deleteSessionState
   * @description Удаляет все данные, связанные с сессией, из стора.
   * @param {string} sessionId - ID сессии для удаления.
   */
  deleteSessionState: (sessionId: string) => void;
}

/**
 * Глобальный стор Zustand для хранения ID текущей активной сессии и состояния ветвления UI.
 * Является единым источником правды для всего приложения о том, какой чат открыт и какие ветки выбраны.
 */
export const useSessionStore = create<SessionState>((set) => ({
  currentSessionId: null,
  sessionBranches: new Map(),

  setCurrentSessionId: (sessionId) => set({ currentSessionId: sessionId }),

  setSessions: (sessions) => set(() => {
    const newSessionBranches = new Map<string, Record<string, string>>();
    sessions.forEach(session => {
      newSessionBranches.set(session.sessionId, session.activeBranches || {});
    });
    return { sessionBranches: newSessionBranches };
  }),

  setActiveBranch: (sessionId, parentId, childId) => set((state) => {
    const newBranches = new Map(state.sessionBranches);
    const sessionSpecificBranches = { ...(newBranches.get(sessionId) || {}) };
    sessionSpecificBranches[parentId] = childId;
    newBranches.set(sessionId, sessionSpecificBranches);
    return { sessionBranches: newBranches };
  }),

  deleteSessionState: (sessionId) => set((state) => {
    const newBranches = new Map(state.sessionBranches);
    newBranches.delete(sessionId);
    return { sessionBranches: newBranches };
  }),
}));
