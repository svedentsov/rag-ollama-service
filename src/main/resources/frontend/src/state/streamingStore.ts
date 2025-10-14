import { create } from 'zustand';
import { ThinkingStep } from '../types';

/**
 * @interface TaskState
 * @description Определяет полную структуру состояния для одной активной задачи/стрима.
 */
export interface TaskState {
  /** @property {string | null} taskId - ID задачи, полученный от сервера. */
  taskId: string | null;
  /** @property {string | null} statusText - Текст статуса для отображения (например, "Ищу информацию..."). */
  statusText: string | null;
  /** @property {Map<string, ThinkingStep>} thinkingSteps - Шаги выполнения плана ("мысли" AI). */
  thinkingSteps: Map<string, ThinkingStep>;
  /** @property {number | null} startTime - Временная метка начала выполнения задачи. */
  startTime: number | null;
}

/**
 * @interface StreamingState
 * @description Определяет структуру глобального стора для отслеживания всех активных стримов.
 */
interface StreamingState {
  /** @property {Map<string, TaskState>} activeStreams - Карта активных стримов, где ключ - ID сообщения ассистента. */
  activeStreams: Map<string, TaskState>;
  /**
   * @function startStream
   * @description Инициализирует состояние для нового стрима.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   */
  startStream: (assistantMessageId: string) => void;
  /**
   * @function stopStream
   * @description Удаляет стрим из активных.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   */
  stopStream: (assistantMessageId: string) => void;
  /**
   * @function updateStreamState
   * @description Обновляет состояние конкретного стрима.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   * @param {Partial<TaskState>} updates - Частичное состояние для обновления.
   */
  updateStreamState: (assistantMessageId: string, updates: Partial<TaskState>) => void;
}

/**
 * Глобальный стор Zustand для управления состоянием всех активных стримов.
 * Является единым источником правды о том, какие задачи выполняются и каков их прогресс.
 */
export const useStreamingStore = create<StreamingState>((set) => ({
  activeStreams: new Map(),

  startStream: (assistantMessageId) =>
    set((state) => {
      const newStreams = new Map(state.activeStreams);
      newStreams.set(assistantMessageId, {
        taskId: null,
        statusText: "Анализирую ваш запрос...",
        thinkingSteps: new Map(),
        startTime: Date.now(), // Сохраняем время старта
      });
      return { activeStreams: newStreams };
    }),

  stopStream: (assistantMessageId) =>
    set((state) => {
      const newStreams = new Map(state.activeStreams);
      newStreams.delete(assistantMessageId);
      return { activeStreams: newStreams };
    }),

  updateStreamState: (assistantMessageId, updates) =>
    set((state) => {
      const currentStream = state.activeStreams.get(assistantMessageId);
      if (!currentStream) return state;

      const newStreams = new Map(state.activeStreams);
      const newThinkingSteps = updates.thinkingSteps
        ? new Map([...currentStream.thinkingSteps, ...updates.thinkingSteps])
        : currentStream.thinkingSteps;

      newStreams.set(assistantMessageId, {
        ...currentStream,
        ...updates,
        thinkingSteps: newThinkingSteps
      });
      return { activeStreams: newStreams };
    }),
}));
