import { useRef, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse } from '../types';
import { streamChatResponse } from '../api';
import { useAddNotification } from '../state/notificationStore';

/**
 * Хук для управления множественными, параллельными потоками ответов чата.
 * Инкапсулирует логику запуска, обработки событий (SSE), обновления кэша
 * React Query в реальном времени и отмены запросов.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const addNotification = useAddNotification();

  /**
   * Обновляет кэш сообщений React Query на основе входящего события из потока.
   * Эта функция обернута в useCallback для стабильности ссылок.
   * @param sessionId ID сессии, для которой пришло обновление.
   * @param assistantMessageId ID сообщения-плейсхолдера ассистента, которое нужно обновить.
   * @param event Событие из потока SSE.
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
   * @param sessionId ID сессии чата.
   * @param query Текст запроса пользователя.
   * @param assistantMessageId Клиентский ID для сообщения-плейсхолдера ассистента.
   * @param currentSessionId ID текущей активной сессии в UI.
   */
  const startStream = useCallback(async (
    sessionId: string,
    query: string,
    assistantMessageId: string,
    currentSessionId: string | null
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
      if (sessionId !== currentSessionId) {
        addNotification(sessionId);
      }
      await queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    }
  }, [queryClient, updateQueryCache, addNotification]);

  /**
   * Отменяет активный поток генерации по ID сообщения ассистента.
   * @param assistantMessageId ID сообщения, генерацию которого нужно остановить.
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
