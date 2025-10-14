import { useRef, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse, ThinkingStep } from '../types';
import { streamChatResponse } from '../api';
import { useNotificationStore } from '../state/notificationStore';
import { useSessionStore } from '../state/sessionStore';
import { useStreamingStore } from '../state/streamingStore';

/**
 * Хук для управления множественными, параллельными потоками ответов чата.
 * Он отвечает за запуск, обработку событий, остановку и финальную синхронизацию состояния.
 * @returns {object} Объект с функциями `startStream` и `stopStream`.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const { addNotification } = useNotificationStore();
  const { startStream: startStreamInStore, stopStream: stopStreamInStore, updateStreamState } = useStreamingStore();

  /**
   * Обновляет кэш React Query и глобальные сторы на основе входящего события из потока.
   * @param {string} sessionId - ID сессии.
   * @param {string} assistantMessageId - ID сообщения ассистента.
   * @param {UniversalStreamResponse} event - Событие из потока.
   */
  const processStreamEvent = useCallback((sessionId: string, assistantMessageId: string, event: UniversalStreamResponse) => {
    queryClient.setQueryData<Message[]>(['messages', sessionId], (oldData = []) =>
      oldData.map(msg => {
        if (msg.id !== assistantMessageId) return msg;
        const updatedMsg: Message = { ...msg };

        switch (event.type) {
          case 'task_started':
            updatedMsg.taskId = event.taskId;
            updateStreamState(assistantMessageId, { taskId: event.taskId });
            break;
          case 'status_update':
            updateStreamState(assistantMessageId, { statusText: event.text });
            break;
          case 'thinking_thought':
            const newStep: ThinkingStep = { name: event.stepName, status: event.status };
            updateStreamState(assistantMessageId, {
              statusText: event.status === 'RUNNING' ? `Выполняю: ${event.stepName}...` : null,
              thinkingSteps: new Map([[event.stepName, newStep]])
            });
            break;
          case 'content':
            updateStreamState(assistantMessageId, { statusText: null, thinkingSteps: new Map() });
            updatedMsg.text = (updatedMsg.text || '') + event.text;
            break;
          case 'sources':
            updatedMsg.sources = event.sources;
            updatedMsg.queryFormationHistory = event.queryFormationHistory;
            updatedMsg.finalPrompt = event.finalPrompt;
            break;
          case 'code':
             updateStreamState(assistantMessageId, { statusText: null, thinkingSteps: new Map() });
            updatedMsg.text = event.generatedCode;
            updatedMsg.finalPrompt = event.finalPrompt;
            break;
          case 'error':
            updatedMsg.error = event.message;
            break;
          case 'done':
            updatedMsg.isStreaming = false;
            break;
        }
        return updatedMsg;
      })
    );
  }, [queryClient, updateStreamState]);

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
    startStreamInStore(assistantMessageId);
    const abortController = new AbortController();
    abortControllersRef.current.set(assistantMessageId, abortController);

    try {
      for await (const event of streamChatResponse(query, sessionId, abortController.signal)) {
        processStreamEvent(sessionId, assistantMessageId, event);
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        toast.error((error as Error).message || 'Произошла неизвестная ошибка в потоке.');
        processStreamEvent(sessionId, assistantMessageId, { type: 'error', message: (error as Error).message });
      }
    } finally {
      // Финальная очистка состояния
      stopStreamInStore(assistantMessageId);
      abortControllersRef.current.delete(assistantMessageId);

      queryClient.setQueryData<Message[]>(['messages', sessionId], (oldData = []) =>
        oldData.map(msg =>
          msg.id === assistantMessageId ? { ...msg, isStreaming: false } : msg
        )
      );

      const currentSessionId = useSessionStore.getState().currentSessionId;
      if (sessionId !== currentSessionId) {
        addNotification(sessionId);
      }
      await queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      await queryClient.invalidateQueries({ queryKey: ['messages', sessionId] });
    }
  }, [processStreamEvent, addNotification, startStreamInStore, stopStreamInStore, queryClient]);

  /**
   * Инициирует остановку генерации ответа для конкретного сообщения.
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
