import { create } from 'zustand';

/**
 * @interface FileSelectionState
 * @description Определяет состояние и действия для управления выбором файлов в разных сессиях чата.
 */
interface FileSelectionState {
  /** Карта, где ключ - ID сессии, а значение - Set с ID выбранных файлов. */
  selections: Map<string, Set<string>>;
  /** Выбирает или отменяет выбор файла для указанной сессии. */
  toggleSelection: (sessionId: string, fileId: string) => void;
  /** Очищает выбор файлов для указанной сессии. */
  clearSelection: (sessionId: string) => void;
  /** Возвращает Set с ID выбранных файлов для указанной сессии. */
  getSelectedIds: (sessionId: string) => Set<string>;
}

/**
 * Глобальный стор Zustand для управления файлами, выбранными для использования в качестве контекста в каждой сессии чата.
 * Является единым источником правды для межфункционального взаимодействия (например, выбор файла в FileManager для использования в Chat).
 */
export const useFileSelectionStore = create<FileSelectionState>((set, get) => ({
  selections: new Map(),

  toggleSelection: (sessionId, fileId) => set(state => {
    const newSelections = new Map(state.selections);
    const sessionSelection = new Set(newSelections.get(sessionId) || []);
    if (sessionSelection.has(fileId)) {
      sessionSelection.delete(fileId);
    } else {
      sessionSelection.add(fileId);
    }
    newSelections.set(sessionId, sessionSelection);
    return { selections: newSelections };
  }),

  clearSelection: (sessionId) => set(state => {
    const newSelections = new Map(state.selections);
    newSelections.delete(sessionId);
    return { selections: newSelections };
  }),

  getSelectedIds: (sessionId) => {
    return get().selections.get(sessionId) || new Set();
  }
}));
