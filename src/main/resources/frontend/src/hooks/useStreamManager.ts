import { useRef, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse } from '../types';
import { streamChatResponse } from '../api';
import { useNotificationStore } from '../state/notificationStore';
import { useSessionStore } from '../state/sessionStore';

/**
 * Хук для управления множественными, параллельными потоками ответов чата.
 * @returns {object} Объект с функциями `startStream` и `stopStream`.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const { addNotification } = useNotificationStore();

  /**
   * Обновляет кэш React Query на основе входящего события из потока.
   */
  const updateQueryCache = useCallback((sessionId: string, assistantMessageId: string, event: UniversalStreamResponse) => {
    const queryKey = ['messages', sessionId];
    queryClient.setQueryData<Message[]>(queryKey, (oldData = []) =>
      oldData.map(msg => {
        if (msg.id !== assistantMessageId) return msg;

        const updatedMsg = { ...msg };
        switch (event.type) {
          case 'task_started':
            updatedMsg.taskId = event.taskId;
            break;
          case 'content':
            updatedMsg.text += event.text;
            break;
          case 'sources':
            updatedMsg.sources = event.sources;
            break;
          case 'error':
            updatedMsg.error = event.message;
            break;
        }
        return updatedMsg;
      })
    );
  }, [queryClient]);

  /**
   * Запускает новый поток для генерации ответа.
   * @param {string} sessionId - ID сессии.
   * @param {string} query - Текст запроса пользователя.
   * @param {string} assistantMessageId - ID сообщения-плейсхолдера для ассистента.
   */
  const startStream = useCallback(async (
    sessionId: string,
    query: string,
    assistantMessageId: string
  ) => {
    const abortController = new AbortController();
    abortControllersRef.current.set(assistantMessageId, abortController);

    try {
      for await (const event of streamChatResponse(query, sessionId, abortController.signal)) {
        updateQueryCache(sessionId, assistantMessageId, event);
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        toast.error((error as Error).message || 'Произошла неизвестная ошибка в потоке.');
        updateQueryCache(sessionId, assistantMessageId, { type: 'error', message: (error as Error).message });
      }
    } finally {
      queryClient.setQueryData<Message[]>(['messages', sessionId], (oldData = []) =>
        oldData.map(msg =>
          msg.id === assistantMessageId ? { ...msg, isStreaming: false } : msg
        )
      );
      abortControllersRef.current.delete(assistantMessageId);

      const currentSessionId = useSessionStore.getState().currentSessionId;
      if (sessionId !== currentSessionId) {
        addNotification(sessionId);
      }

      await queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    }
  }, [queryClient, updateQueryCache, addNotification]);

  /**
   * Останавливает генерацию ответа для конкретного сообщения.
   * @param {string} assistantMessageId - ID сообщения ассистента, генерацию которого нужно остановить.
   */
  const stopStream = useCallback((assistantMessageId: string) => {
    const controller = abortControllersRef.current.get(assistantMessageId);
    if (controller) {
      controller.abort();
      toast.success('Генерация ответа остановлена.');
    }
  }, []);

  return { startStream, stopStream };
}
