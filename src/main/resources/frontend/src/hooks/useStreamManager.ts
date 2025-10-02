import { useRef, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse } from '../types';
import { streamChatResponse } from '../api';
import { useAddNotification } from '../state/notificationStore';

/**
 * Вспомогательная функция для получения текущего sessionId напрямую из URL.
 * Это позволяет избежать проблемы "устаревшего замыкания" (stale closure).
 * @returns ID активной сессии или null.
 */
const getActiveSessionIdFromURL = (): string | null => {
  const params = new URLSearchParams(window.location.search);
  return params.get('sessionId');
};

/**
 * Хук для управления множественными, параллельными потоками ответов.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const addNotification = useAddNotification();

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
      
      // ИЗМЕНЕНИЕ ЗДЕСЬ: Получаем актуальный ID сессии из URL в момент завершения
      const activeSessionId = getActiveSessionIdFromURL();
      if (sessionId !== activeSessionId) {
        addNotification(sessionId);
      }
      
      await queryClient.invalidateQueries({ queryKey: ['messages', sessionId], exact: true });
      await queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    }
  }, [queryClient, updateQueryCache, addNotification]);

  const stopStream = useCallback((assistantMessageId: string) => {
    const controller = abortControllersRef.current.get(assistantMessageId);
    if (controller) {
      controller.abort();
      toast.success('Генерация ответа остановлена.');
    }
  }, []);

  return { startStream, stopStream };
}
