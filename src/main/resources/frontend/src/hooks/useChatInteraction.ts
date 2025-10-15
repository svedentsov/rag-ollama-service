import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { useStreamManager } from './useStreamManager';
import { Message } from '../types';
import { useChatMessages } from './useChatMessages';
import { useStreamingStore } from '../state/streamingStore';
import { useAttachmentStore } from '../state/attachmentStore';

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
  const activeStreams = useStreamingStore((state) => state.activeStreams);
  const { clearAttachment } = useAttachmentStore();

  /**
   * Отправляет новое сообщение от пользователя и инициирует поток ответа от ассистента.
   */
  const handleSendMessage = useCallback((inputText: string, context?: string) => {
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText, createdAt: new Date().toISOString(), context };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true, createdAt: new Date().toISOString() };

    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, userMessage, assistantMessage]);
    startStream(sessionId, inputText, assistantMessage.id, messages, context);

    // Очищаем аттачмент только после успешной отправки
    if (context && sessionId) {
        clearAttachment(sessionId);
    }
  }, [queryClient, sessionId, startStream, messages, clearAttachment]);

  /**
   * Запускает повторную генерацию для существующего ответа ассистента.
   */
  const handleRegenerate = useCallback((assistantMessageToRegenerate: Message) => {
    if (!assistantMessageToRegenerate.parentId) return;

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
  }, [messages, queryClient, sessionId, startStream]);

  /**
   * Останавливает все активные генерации ответов.
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
  };
}
