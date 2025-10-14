import { create } from 'zustand';
import { ThinkingStep } from '../types';

/**
 * @interface ThinkingState
 * @description Определяет структуру состояния для отслеживания процесса "мышления" AI.
 */
interface ThinkingState {
  /** @property {boolean} isVisible - Флаг, определяющий, должен ли отображаться UI "мышления". */
  isVisible: boolean;
  /** @property {Map<string, ThinkingStep>} steps - Карта шагов плана. Ключ - имя шага, значение - объект шага. */
  steps: Map<string, ThinkingStep>;
  /**
   * @function startThinking
   * @description Действие для начала процесса "мышления", очищает предыдущее состояние.
   */
  startThinking: () => void;
  /**
   * @function stopThinking
   * @description Действие для завершения/скрытия процесса "мышления".
   */
  stopThinking: () => void;
  /**
   * @function updateStep
   * @description Действие для добавления нового или обновления существующего статуса шага.
   * @param {ThinkingStep} step - Объект шага с его именем и статусом.
   */
  updateStep: (step: ThinkingStep) => void;
}

/**
 * Глобальный стор Zustand для управления состоянием "Thinking Thoughts".
 * Позволяет компоненту визуализации подписываться только на релевантные ему изменения,
 * оптимизируя перерисовки.
 */
export const useThinkingStore = create<ThinkingState>((set) => ({
  isVisible: false,
  steps: new Map(),
  startThinking: () => set({ isVisible: true, steps: new Map() }),
  stopThinking: () => set({ isVisible: false }),
  updateStep: (step) =>
    set((state) => ({
      // Важно создавать новую карту, чтобы React обнаружил изменение состояния
      steps: new Map(state.steps).set(step.name, step),
    })),
}));
