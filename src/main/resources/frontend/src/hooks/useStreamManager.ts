import { useRef, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse } from '../types';
import { streamChatResponse } from '../api';

/**
 * Хук для управления множественными, параллельными потоками ответов.
 * Отвечает за запуск, отмену и обновление кэша для каждого потока индивидуально.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  // Используем Ref для хранения AbortController'ов, чтобы не вызывать ререндер при их изменении.
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());

  /**
   * Обновляет конкретное сообщение в кэше TanStack Query на основе пришедшего события.
   * @param sessionId - ID сессии, к которой относится сообщение.
   * @param assistantMessageId - ID сообщения ассистента, которое нужно обновить.
   * @param event - Событие из SSE потока.
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
   */
  const startStream = useCallback(async (
    sessionId: string,
    query: string,
    assistantMessageId: string
  ) => {
    const abortController = new AbortController();
    abortControllersRef.current.set(assistantMessageId, abortController);

    try {
      // Итерируемся по асинхронному генератору
      for await (const event of streamChatResponse(query, sessionId, abortController.signal)) {
        updateQueryCache(sessionId, assistantMessageId, event);
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        toast.error((error as Error).message || 'Произошла неизвестная ошибка в потоке.');
        updateQueryCache(sessionId, assistantMessageId, { type: 'error', message: (error as Error).message });
      }
    } finally {
      // Завершаем стриминг для конкретного сообщения и чистим AbortController
      queryClient.setQueryData<Message[]>(['messages', sessionId], (oldData = []) =>
        oldData.map(msg =>
          msg.id === assistantMessageId ? { ...msg, isStreaming: false } : msg
        )
      );
      abortControllersRef.current.delete(assistantMessageId);
      // Финальная сверка с сервером
      await queryClient.invalidateQueries({ queryKey: ['messages', sessionId] });
      await queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    }
  }, [queryClient, updateQueryCache]);

  /**
   * Отменяет конкретный поток по ID сообщения.
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
