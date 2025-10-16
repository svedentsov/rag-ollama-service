import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { useStreamManager } from './useStreamManager';
import { Message } from '../types';
import { useStreamingStore } from '../state/streamingStore';
import { useAttachmentStore } from '../state/attachmentStore';

/**
 * @description Хук, инкапсулирующий всю бизнес-логику действий пользователя в чате.
 * Он отвечает за отправку, регенерацию и остановку сообщений, делегируя сложную
 * логику управления потоками хуку `useStreamManager`.
 *
 * @param {string} sessionId - ID текущей сессии чата.
 * @returns {{
 *   handleSendMessage: (inputText: string, context?: string) => void,
 *   handleRegenerate: (assistantMessageToRegenerate: Message) => void,
 *   handleStopGenerating: () => void,
 *   stopStream: (assistantMessageId: string) => void,
 *   isStreaming: boolean
 * }} Объект с функциями для взаимодействия с чатом и флагом активности.
 */
export function useChatInteraction(sessionId: string) {
  const queryClient = useQueryClient();
  const { startStream, stopStream } = useStreamManager();
  const activeStreams = useStreamingStore((state) => state.activeStreams);
  const isStreaming = activeStreams.size > 0;
  const { clearAttachment } = useAttachmentStore();

  /**
   * @description Отправляет новое сообщение от пользователя и инициирует поток ответа от ассистента.
   * @param {string} inputText - Текст сообщения пользователя.
   * @param {string} [context] - Опциональный строковый контекст (например, содержимое файла).
   */
  const handleSendMessage = useCallback((inputText: string, context?: string) => {
    const userMessage: Message = {
      id: uuidv4(),
      type: 'user',
      text: inputText,
      createdAt: new Date().toISOString(),
      context
    };

    const assistantMessage: Message = {
      id: uuidv4(),
      type: 'assistant',
      text: '',
      parentId: userMessage.id,
      isStreaming: true,
      createdAt: new Date().toISOString()
    };

    // Оптимистичное обновление UI
    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, userMessage, assistantMessage]);

    // Получаем актуальную историю ПЕРЕД запуском стрима
    const currentMessages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    startStream(sessionId, inputText, assistantMessage.id, currentMessages.slice(0, -1), context);

    if (context && sessionId) {
        clearAttachment(sessionId);
    }
  }, [queryClient, sessionId, startStream, clearAttachment]);

  /**
   * @description Запускает повторную генерацию для существующего ответа ассистента.
   * @param {Message} assistantMessageToRegenerate - Объект сообщения ассистента, которое нужно перегенерировать.
   */
  const handleRegenerate = useCallback((assistantMessageToRegenerate: Message) => {
    if (!assistantMessageToRegenerate.parentId) return;

    const messages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    const parentMessageIndex = messages.findIndex(m => m.id === assistantMessageToRegenerate.parentId);

    if (parentMessageIndex > -1) {
        const parentMessage = messages[parentMessageIndex];
        const historyUpToParent = messages.slice(0, parentMessageIndex + 1);
        const newAssistantMessage: Message = {
            id: uuidv4(),
            type: 'assistant',
            text: '',
            parentId: parentMessage.id,
            isStreaming: true,
            createdAt: new Date().toISOString()
        };
        queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
        startStream(sessionId, parentMessage.text, newAssistantMessage.id, historyUpToParent, parentMessage.context);
    }
  }, [queryClient, sessionId, startStream]);

  /**
   * @description Останавливает все активные генерации ответов.
   */
  const handleStopGenerating = useCallback(() => {
    if (activeStreams.size > 0) {
      for (const messageId of activeStreams.keys()) {
        stopStream(messageId);
      }
    } else {
        console.warn("Stop generating called, but no active streams found in the global store.");
    }
  }, [activeStreams, stopStream]);

  return {
    handleSendMessage,
    handleRegenerate,
    handleStopGenerating,
    stopStream,
    isStreaming
  };
}
