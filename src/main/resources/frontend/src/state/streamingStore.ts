import { create } from 'zustand';
import { ThinkingStep } from '../types';

/**
 * @interface TaskState
 * @description Определяет полную структуру состояния для одной активной асинхронной задачи или потока данных (stream).
 */
export interface TaskState {
  /** @property {string | null} taskId - ID задачи, полученный от сервера. */
  taskId: string | null;
  /** @property {string | null} statusText - Текст текущего статуса для отображения (например, "Ищу информацию..."). */
  statusText: string | null;
  /** @property {Map<string, ThinkingStep>} thinkingSteps - Карта шагов выполнения плана ("мысли" AI), где ключ - имя шага. */
  thinkingSteps: Map<string, ThinkingStep>;
  /** @property {number | null} startTime - Временная метка начала выполнения задачи в миллисекундах. */
  startTime: number | null;
}

/**
 * @interface StreamingState
 * @description Определяет структуру глобального стора для отслеживания всех активных потоков данных (стримов).
 */
interface StreamingState {
  /** @property {Map<string, TaskState>} activeStreams - Карта активных стримов, где ключ - это временный ID сообщения ассистента. */
  activeStreams: Map<string, TaskState>;
  /**
   * @function startStream
   * @description Инициализирует состояние для нового стрима при начале генерации ответа.
   * @param {string} assistantMessageId - ID сообщения ассистента, к которому привязан стрим.
   */
  startStream: (assistantMessageId: string) => void;
  /**
   * @function stopStream
   * @description Удаляет стрим из активных по его завершении или отмене.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   */
  stopStream: (assistantMessageId: string) => void;
  /**
   * @function updateStreamState
   * @description Обновляет состояние конкретного активного стрима новыми данными.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   * @param {Partial<TaskState>} updates - Частичный объект состояния для обновления.
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
        startTime: Date.now(),
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
        thinkingSteps: newThinkingSteps,
      });
      return { activeStreams: newStreams };
    }),
}));
