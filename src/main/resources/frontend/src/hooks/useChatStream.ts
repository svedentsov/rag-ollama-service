import React from 'react';
import { useMutation, QueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Message, UniversalStreamResponse } from '../types';

/**
 * Параметры для хука useChatStream.
 */
interface UseChatStreamProps {
  /** @param sessionId - ID текущей сессии чата. */
  sessionId: string;
  /** @param queryClient - Экземпляр клиента TanStack Query. */
  queryClient: QueryClient;
  /** @param onStreamEnd - Колбэк, вызываемый при завершении потока. */
  onStreamEnd?: () => void;
  /** @param onTaskStart - Колбэк, вызываемый при получении ID задачи. */
  onTaskStart?: (assistantMessageId: string, taskId: string) => void;
  /** @param onStatusUpdate - Колбэк для обновления текста статуса. */
  onStatusUpdate?: (text: string) => void;
}

/**
 * Хук для управления потоковой передачей ответа от чата.
 * Он инкапсулирует логику выполнения запроса, обработки SSE,
 * прямого обновления кэша React Query и управления отменой.
 * @param {UseChatStreamProps} props - Параметры хука.
 */
export function useChatStream({
  sessionId,
  queryClient,
  onStreamEnd,
  onTaskStart,
  onStatusUpdate,
}: UseChatStreamProps) {
  const abortControllerRef = React.useRef<AbortController | null>(null);

  const mutation = useMutation({
    mutationFn: async ({ query, assistantMessageId }: { query: string; assistantMessageId: string }) => {
      abortControllerRef.current = new AbortController();
      const response = await fetch('/api/v1/orchestrator/ask-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query, sessionId }),
        signal: abortControllerRef.current.signal,
      });

      if (!response.body) throw new Error('Stream body is missing');
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error! Status: ${response.status}, message: ${errorText}`);
      }

      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
      let buffer = '';

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += value;
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const jsonData = line.substring(5).trim();
            if (!jsonData) continue;
            try {
              const eventData = JSON.parse(jsonData) as UniversalStreamResponse;
              updateQueryCache(assistantMessageId, eventData);

              // Вызываем колбэки для уведомления родительского компонента
              if (eventData.type === 'task_started' && onTaskStart) {
                onTaskStart(assistantMessageId, eventData.taskId);
              }
              if (eventData.type === 'status_update' && onStatusUpdate) {
                onStatusUpdate(eventData.text);
              }

            } catch (e) {
              console.error('Failed to parse SSE data:', jsonData, e);
            }
          }
        }
      }
    },
    onSuccess: () => {
      onStreamEnd?.();
    },
    onError: (error: Error) => {
      if (error.name !== 'AbortError') {
        toast.error(error.message || 'Произошла неизвестная ошибка.');
      }
      onStreamEnd?.();
    },
    onSettled: () => {
      abortControllerRef.current = null;
    },
  });

  const updateQueryCache = (assistantMessageId: string, event: UniversalStreamResponse) => {
    const queryKey = ['messages', sessionId];

    queryClient.setQueryData<Message[]>(queryKey, (oldData = []) => {
      if (!oldData) return [];
      return oldData.map(msg => {
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
      });
    });
  };

  const stop = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
  };

  return { ...mutation, stop };
}
