import { create } from 'zustand';

/**
 * @interface StreamingState
 * @description Определяет структуру состояния для отслеживания активных стримов.
 */
interface StreamingState {
  /** @property {Set<string>} streamingMessageIds - Множество ID сообщений ассистента, которые в данный момент генерируются. */
  streamingMessageIds: Set<string>;
  /**
   * @function addStreamingMessage
   * @description Добавляет ID сообщения в множество активных стримов.
   * @param {string} messageId - ID сообщения.
   */
  addStreamingMessage: (messageId: string) => void;
  /**
   * @function removeStreamingMessage
   * @description Удаляет ID сообщения из множества активных стримов.
   * @param {string} messageId - ID сообщения.
   */
  removeStreamingMessage: (messageId: string) => void;
}

/**
 * Глобальный стор Zustand для отслеживания сообщений, находящихся в процессе стриминга.
 * Позволяет компонентам подписываться только на статус загрузки, а не на весь массив сообщений,
 * что значительно повышает производительность.
 */
export const useStreamingStore = create<StreamingState>((set) => ({
  streamingMessageIds: new Set(),
  addStreamingMessage: (messageId) =>
    set((state) => ({
      streamingMessageIds: new Set(state.streamingMessageIds).add(messageId),
    })),
  removeStreamingMessage: (messageId) =>
    set((state) => {
      const newSet = new Set(state.streamingMessageIds);
      newSet.delete(messageId);
      return { streamingMessageIds: newSet };
    }),
}));
