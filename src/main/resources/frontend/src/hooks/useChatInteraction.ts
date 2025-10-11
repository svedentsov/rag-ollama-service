import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { useStreamManager } from './useStreamManager';
import { Message } from '../types';
import { useChatMessages } from './useChatMessages';
import { useStreamingStore } from '../state/streamingStore';

/**
 * Хук, инкапсулирующий всю бизнес-логику действий пользователя в чате.
 * Он отвечает за отправку, регенерацию и остановку сообщений.
 * @param {string} sessionId - ID текущей сессии чата.
 * @returns {object} Объект с функциями для взаимодействия с чатом.
 */
export function useChatInteraction(sessionId: string) {
  const queryClient = useQueryClient();
  const { startStream, stopStream } = useStreamManager();
  const { messages } = useChatMessages(sessionId);

  /**
   * Отправляет новое сообщение от пользователя и инициирует поток ответа от ассистента.
   */
  const handleSendMessage = useCallback((inputText: string) => {
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true };

    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, userMessage, assistantMessage]);
    startStream(sessionId, inputText, assistantMessage.id);
  }, [queryClient, sessionId, startStream]);

  /**
   * Запускает повторную генерацию для существующего ответа ассистента.
   */
  const handleRegenerate = useCallback((assistantMessage: Message) => {
    if (!assistantMessage.parentId) return;

    const parentMessage = messages.find(m => m.id === assistantMessage.parentId);
    if (parentMessage) {
      const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true };

      queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
      startStream(sessionId, parentMessage.text, newAssistantMessage.id);
    }
  }, [messages, queryClient, sessionId, startStream]);

  /**
   * Останавливает активную генерацию ответа в текущей сессии (для основной кнопки "Стоп").
   */
  const handleStopGenerating = useCallback(() => {
    const streamingMessage = messages.find(msg => msg.isStreaming);
    if (streamingMessage) {
      stopStream(streamingMessage.id);
    } else {
      const globalStreamingIds = useStreamingStore.getState().streamingMessageIds;
      const firstId = Array.from(globalStreamingIds)[0];
      if (firstId) {
        console.warn(`Не найдено активного стрима в сессии ${sessionId}. Остановка первого глобального стрима в качестве fallback.`);
        stopStream(firstId);
      }
    }
  }, [messages, stopStream, sessionId]);

  return {
    handleSendMessage,
    handleRegenerate,
    handleStopGenerating,
    stopStream, // Явно экспортируем функцию для использования в дочерних компонентах
  };
}
