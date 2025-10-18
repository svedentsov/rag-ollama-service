import { create } from 'zustand';
import { ThinkingStep } from '../types';

/**
 * @interface ThinkingState
 * @description Определяет структуру состояния для отслеживания процесса "мышления" AI.
 * @deprecated Логика объединена в `useStreamingStore` для большей консистентности.
 */
interface ThinkingState {
  isVisible: boolean;
  steps: Map<string, ThinkingStep>;
  startThinking: () => void;
  stopThinking: () => void;
  updateStep: (step: ThinkingStep) => void;
}

/**
 * Глобальный стор Zustand для управления состоянием "Thinking Thoughts".
 * @deprecated Этот стор больше не используется. Его функциональность интегрирована в `useStreamingStore`.
 */
export const useThinkingStore = create<ThinkingState>((set) => ({
  isVisible: false,
  steps: new Map(),
  startThinking: () => set({ isVisible: true, steps: new Map() }),
  stopThinking: () => set({ isVisible: false }),
  updateStep: (step) =>
    set((state) => ({
      steps: new Map(state.steps).set(step.name, step),
    })),
}));
