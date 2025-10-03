import { ChatSession, Message, UniversalStreamResponse } from './types';

const API_BASE_URL = '/api/v1';

/**
 * @class ApiError
 * @description Кастомный класс ошибки для API-клиента.
 * Содержит статус ответа и тело ошибки для детальной диагностики.
 */
class ApiError extends Error {
  constructor(public status: number, public responseBody: any, message?: string) {
    super(message || `API Error: ${status}`);
    this.name = 'ApiError';
  }
}

/**
 * Централизованный API-клиент.
 * Инкапсулирует логику fetch, обработку ответов и ошибок.
 * @template T - Ожидаемый тип данных в успешном ответе.
 * @param {string} endpoint - Путь к эндпоинту API (без /api/v1).
 * @param {RequestInit} [options] - Опции для fetch.
 * @returns {Promise<T>} Промис с распарсенными данными.
 * @throws {ApiError} В случае ошибки сети или неуспешного статуса ответа.
 */
async function apiClient<T>(endpoint: string, options?: RequestInit): Promise<T> {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options?.headers,
      },
    });

    if (!response.ok) {
      const errorBody = await response.text();
      let errorJson: any;
      try {
        errorJson = JSON.parse(errorBody);
      } catch {
        errorJson = { message: errorBody };
      }
      throw new ApiError(response.status, errorJson, `HTTP error on ${endpoint}`);
    }

    // Обрабатываем пустые ответы (например, при статусе 204 No Content)
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }
    return undefined as T;

  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    // Оборачиваем сетевые ошибки в наш кастомный класс для унифицированной обработки
    throw new ApiError(503, { message: (error as Error).message }, 'Network or fetch error');
  }
}

/**
 * Объект, предоставляющий все методы для взаимодействия с API бэкенда.
 * Является единственной точкой входа для всех HTTP-запросов из приложения.
 */
export const api = {
  /**
   * Запрашивает список всех сессий чата для текущего пользователя.
   * @returns {Promise<ChatSession[]>} Промис со списком сессий.
   */
  getChatSessions: () => apiClient<ChatSession[]>('/chats'),

  /**
   * Создает новую сессию чата.
   * @returns {Promise<ChatSession>} Промис с созданной сессией.
   */
  createNewChat: () => apiClient<ChatSession>('/chats', { method: 'POST' }),

  /**
   * Обновляет имя сессии чата.
   * @param {object} params - Параметры.
   * @param {string} params.sessionId - ID сессии.
   * @param {string} params.newName - Новое имя.
   * @returns {Promise<void>}
   */
  updateChatName: ({ sessionId, newName }: { sessionId: string; newName: string }) =>
    apiClient<void>(`/chats/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify({ newName }),
    }),

  /**
   * Запрашивает историю сообщений для указанной сессии.
   * @param {string} sessionId - ID сессии.
   * @returns {Promise<Message[]>} Промис с массивом сообщений, преобразованным в клиентский формат.
   */
  fetchMessages: (sessionId: string): Promise<Message[]> =>
    apiClient<{ id: string; parentId: string | null; role: 'USER' | 'ASSISTANT'; content: string; taskId?: string }[]>(`/chats/${sessionId}/messages`)
    .then(messages =>
        // Преобразуем DTO с бэкенда в наш клиентский тип Message
        messages.map(msg => ({
            id: msg.id,
            taskId: msg.taskId,
            parentId: msg.parentId ?? undefined,
            type: msg.role === 'USER' ? 'user' : 'assistant',
            text: msg.content,
            sources: [], // `sources` приходят только через SSE, при загрузке истории они пусты
            isStreaming: false,
        }))
    ),

  /**
   * Удаляет сессию чата.
   * @param {string} sessionId - ID сессии для удаления.
   * @returns {Promise<void>}
   */
  deleteChatSession: (sessionId: string) => apiClient<void>(`/chats/${sessionId}`, { method: 'DELETE' }),

  /**
   * Обновляет текст существующего сообщения.
   * @param {object} params - Параметры.
   * @param {string} params.messageId - ID сообщения.
   * @param {string} params.newContent - Новый текст.
   * @returns {Promise<void>}
   */
  updateMessage: ({ messageId, newContent }: { messageId: string; newContent: string }) =>
    apiClient<void>(`/messages/${messageId}`, {
      method: 'PUT',
      body: JSON.stringify({ newContent }),
    }),

  /**
   * Удаляет сообщение.
   * @param {string} messageId - ID сообщения для удаления.
   * @returns {Promise<void>}
   */
  deleteMessage: (messageId: string) => apiClient<void>(`/messages/${messageId}`, { method: 'DELETE' }),

  /**
   * Отправляет обратную связь по задаче.
   * @param {object} params - Параметры.
   * @param {string} params.taskId - ID задачи (полученный из сообщения).
   * @param {boolean} params.isHelpful - Оценка пользователя.
   * @returns {Promise<void>}
   */
  sendFeedback: ({ taskId, isHelpful }: { taskId: string; isHelpful: boolean }) =>
    apiClient<void>('/feedback', {
      method: 'POST',
      body: JSON.stringify({ requestId: taskId, isHelpful }),
    }),
};

/**
 * Низкоуровневая сервисная функция для получения потока событий (SSE).
 * @param query - Запрос пользователя.
 * @param sessionId - ID сессии.
 * @param signal - AbortSignal для отмены запроса.
 * @returns Асинхронный итерируемый объект с событиями.
 */
export async function* streamChatResponse(
    query: string,
    sessionId: string,
    signal: AbortSignal
): AsyncGenerator<UniversalStreamResponse, void, undefined> {
    const response = await fetch('/api/v1/orchestrator/ask-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
        body: JSON.stringify({ query, sessionId }),
        signal,
    });

    if (!response.body) throw new Error('Stream body is missing');
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error! Status: ${response.status}, message: ${errorText}`);
    }

    const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
    let buffer = '';

    try {
      while (true) {
          const { value, done } = await reader.read();
          if (done) break;

          buffer += value;
          const lines = buffer.split('\n\n');
          buffer = lines.pop() || ''; // Последняя (возможно, неполная) часть остается в буфере

          for (const line of lines) {
              if (line.startsWith('data:')) {
                  const jsonData = line.substring(5).trim();
                  if (!jsonData) continue;
                  try {
                      yield JSON.parse(jsonData) as UniversalStreamResponse;
                  } catch (e) {
                      console.error('Failed to parse SSE data chunk:', jsonData, e);
                  }
              }
          }
      }
    } finally {
        reader.releaseLock();
    }
}