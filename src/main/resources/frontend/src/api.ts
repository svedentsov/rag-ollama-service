import { ChatSession, ServerMessageDto, UniversalStreamResponse } from './types';

const API_BASE_URL = '/api/v1';

/**
 * @class ApiError
 * @description Кастомный класс ошибки для API-клиента. Содержит HTTP-статус и тело ответа для удобной отладки.
 */
class ApiError extends Error {
  /**
   * @param {number} status - HTTP-статус ответа.
   * @param {any} responseBody - Тело ответа сервера.
   * @param {string} [message] - Сообщение об ошибке.
   */
  constructor(public status: number, public responseBody: any, message?: string) {
    super(message || `API Error: ${status}`);
    this.name = 'ApiError';
  }
}

/**
 * Централизованный, робастный API-клиент для взаимодействия с бэкендом.
 * Отвечает исключительно за сетевые запросы и обработку HTTP-статусов.
 * Не занимается трансформацией данных.
 * @template T - Ожидаемый тип данных в успешном ответе (серверный DTO).
 * @param {string} endpoint - Путь к эндпоинту API (например, '/chats').
 * @param {RequestInit} [options] - Стандартные опции для `fetch`.
 * @returns {Promise<T>} Промис, который разрешается с "сырыми" данными от сервера.
 * @throws {ApiError} В случае любой сетевой ошибки или неуспешного HTTP-статуса.
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

    if (response.status === 202 || response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }

    return undefined as T;

  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(503, { message: (error as Error).message }, 'Network or fetch error');
  }
}

/**
 * Объект, предоставляющий все методы для взаимодействия с API бэкенда.
 * Каждый метод использует централизованный `apiClient` для выполнения запросов.
 */
export const api = {
  /**
   * Получает список всех сессий чата для текущего пользователя.
   * @returns {Promise<ChatSession[]>} Массив сессий чата.
   */
  getChatSessions: () => apiClient<ChatSession[]>('/chats'),
  /**
   * Создает новую сессию чата.
   * @returns {Promise<ChatSession>} Созданная сессия чата.
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
   * Загружает "сырую" историю сообщений (DTO) для указанной сессии.
   * @param {string} sessionId - ID сессии.
   * @returns {Promise<ServerMessageDto[]>} Массив серверных DTO сообщений.
   */
  fetchMessages: (sessionId: string): Promise<ServerMessageDto[]> =>
    apiClient<ServerMessageDto[]>(`/chats/${sessionId}/messages`),
  /**
   * Удаляет сессию чата.
   * @param {string} sessionId - ID сессии.
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
   * @param {string} messageId - ID сообщения.
   * @returns {Promise<void>}
   */
  deleteMessage: (messageId: string) => apiClient<void>(`/messages/${messageId}`, { method: 'DELETE' }),
  /**
   * Отправляет обратную связь по ответу.
   * @param {object} params - Параметры.
   * @param {string} params.taskId - ID задачи, сгенерировавшей ответ.
   * @param {boolean} params.isHelpful - Оценка пользователя.
   * @returns {Promise<void>}
   */
  sendFeedback: ({ taskId, isHelpful }: { taskId: string; isHelpful: boolean }) =>
    apiClient<void>('/feedback', {
      method: 'POST',
      body: JSON.stringify({ requestId: taskId, isHelpful }),
    }),
  /**
   * Устанавливает активную ветку для ответа в диалоге.
   * @param {object} params - Параметры.
   * @param {string} params.sessionId - ID сессии.
   * @param {string} params.parentId - ID родительского сообщения.
   * @param {string} params.childId - ID дочернего сообщения, которое становится активным.
   * @returns {Promise<void>}
   */
  setActiveBranch: ({ sessionId, parentId, childId }: { sessionId: string; parentId: string; childId: string }) =>
    apiClient<void>(`/chats/${sessionId}/active-branch`, {
        method: 'PUT',
        body: JSON.stringify({ parentMessageId: parentId, activeChildId: childId }),
    }),
};

/**
 * Низкоуровневая сервисная функция для получения потока событий (SSE).
 * @param {string} query - Текст запроса пользователя.
 * @param {string} sessionId - ID текущей сессии.
 * @param {AbortSignal} signal - Сигнал для отмены запроса.
 * @returns {AsyncGenerator<UniversalStreamResponse, void, undefined>} Асинхронный генератор, который выдает части ответа.
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
          buffer = lines.pop() || '';

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
