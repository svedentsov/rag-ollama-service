import { create } from 'zustand';

/**
 * @interface BranchSelectionState
 * @description Определяет структуру состояния для управления выбором веток ответов.
 */
interface BranchSelectionState {
  /** @property {Record<string, string>} selections - Карта, где ключ - ID родительского сообщения, а значение - ID выбранного дочернего сообщения. */
  selections: Record<string, string>;
  /**
   * @function selectBranch
   * @description Действие для выбора активной ветки (дочернего сообщения) для указанного родителя.
   * @param {string} parentId - ID родительского сообщения.
   * @param {string} childId - ID дочернего сообщения, которое нужно сделать активным.
   */
  selectBranch: (parentId: string, childId: string) => void;
}

/**
 * Создает и экспортирует стор Zustand для управления выбором веток.
 * Этот стор инкапсулирует состояние и логику его изменения,
 * позволяя компонентам подписываться на него напрямую.
 */
export const useBranchSelectionStore = create<BranchSelectionState>((set) => ({
  selections: {},
  selectBranch: (parentId, childId) =>
    set((state) => ({
      selections: {
        ...state.selections,
        [parentId]: childId,
      },
    })),
}));
