import { useRef, useCallback } from 'react';
import { useQueryClient, QueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse, ThinkingStep } from '../types';
import { streamChatResponse } from '../api';
import { useNotificationStore } from '../state/notificationStore';
import { useSessionStore } from '../state/sessionStore';
import { useStreamingStore } from '../state/streamingStore';

/**
 * Чистая функция для обработки одного события из потока (SSE).
 * @param queryClient - Инстанс клиента React Query.
 * @param updateStreamState - Функция для обновления стора стриминга.
 * @param sessionId - ID сессии чата.
 * @param assistantMessageId - ID сообщения ассистента, которое обновляется.
 * @param event - Событие из потока SSE.
 */
const processStreamEvent = (
    queryClient: QueryClient,
    updateStreamState: (assistantMessageId: string, updates: Partial<import('../state/streamingStore').TaskState>) => void,
    sessionId: string,
    assistantMessageId: string,
    event: UniversalStreamResponse
) => {
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
                    updatedMsg.trustScoreReport = event.trustScoreReport;
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
};

/**
 * Хук для управления жизненным циклом потоков данных (SSE).
 * @returns Объект с функциями `startStream` и `stopStream`.
 */
export function useStreamManager() {
  const queryClient = useQueryClient();
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const { addNotification } = useNotificationStore();
  const { startStream: startStreamInStore, stopStream: stopStreamInStore, updateStreamState } = useStreamingStore();

  const startStream = useCallback(async (
    sessionId: string,
    query: string,
    assistantMessageId: string,
    history: Message[],
    fileIds?: string[]
  ) => {
    startStreamInStore(assistantMessageId);
    const abortController = new AbortController();
    abortControllersRef.current.set(assistantMessageId, abortController);

    try {
      for await (const event of streamChatResponse(query, sessionId, abortController.signal, history, fileIds)) {
        processStreamEvent(queryClient, updateStreamState, sessionId, assistantMessageId, event);
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        toast.error((error as Error).message || 'Произошла неизвестная ошибка в потоке.');
        processStreamEvent(queryClient, updateStreamState, sessionId, assistantMessageId, { type: 'error', message: (error as Error).message });
      }
    } finally {
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
  }, [queryClient, updateStreamState, addNotification, startStreamInStore, stopStreamInStore]);

  const stopStream = useCallback((assistantMessageId: string) => {
    const controller = abortControllersRef.current.get(assistantMessageId);
    if (controller) {
      controller.abort();
      toast.success('Генерация ответа остановлена.');
    }
  }, []);

  return { startStream, stopStream };
}
