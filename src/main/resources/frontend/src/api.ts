import { ChatSession, ServerMessageDto, UniversalStreamResponse, Message, FileDto } from './types';

const API_BASE_URL = '/api/v1';

/**
 * @class ApiError
 * @description Кастомный класс ошибки для API-клиента. Содержит HTTP-статус и тело ответа для удобной отладки.
 */
class ApiError extends Error {
  constructor(public status: number, public responseBody: any, message?: string) {
    super(message || `API Error: ${status}`);
    this.name = 'ApiError';
  }
}

/**
 * Централизованный, робастный API-клиент для взаимодействия с бэкендом по JSON.
 * @template T - Ожидаемый тип данных в успешном ответе.
 * @param {string} endpoint - Путь к эндпоинту API (например, '/chats').
 * @param {RequestInit} [options] - Стандартные опции для `fetch`.
 * @returns {Promise<T>} Промис, который разрешается с данными от сервера.
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

    // Для ответов без тела (202, 204) возвращаем undefined
    if (response.status === 202 || response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }
    // Для ответов с телом, но без JSON (например, plain/text)
    return undefined as T;

  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    // Оборачиваем сетевые ошибки и другие исключения fetch в наш кастомный тип
    throw new ApiError(503, { message: (error as Error).message }, 'Network or fetch error');
  }
}

/**
 * Специализированный клиент для отправки multipart/form-data (загрузка файлов).
 * @template T - Ожидаемый тип данных в успешном ответе.
 * @param {string} endpoint - Путь к эндпоинту API.
 * @param {FormData} formData - Объект FormData с файлом.
 * @returns {Promise<T>} Промис с ответом сервера.
 * @throws {ApiError} В случае ошибки.
 */
async function apiClientMultipart<T>(endpoint: string, formData: FormData): Promise<T> {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            body: formData,
            // Заголовок 'Content-Type' устанавливается браузером автоматически для FormData
        });

        if (!response.ok) {
            const errorBody = await response.json();
            throw new ApiError(response.status, errorBody, `HTTP error on ${endpoint}`);
        }

        return response.json();
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(503, { message: (error as Error).message }, 'Network or fetch error');
    }
}

/**
 * Объект-фасад, предоставляющий все типизированные методы для взаимодействия с API бэкенда.
 */
export const api = {
  /** Получает список всех сессий чата для текущего пользователя. */
  getChatSessions: () => apiClient<ChatSession[]>('/chats'),
  /** Создает новую сессию чата. */
  createNewChat: () => apiClient<ChatSession>('/chats', { method: 'POST' }),
  /**
   * Обновляет имя указанной сессии чата.
   * @param {object} params - Параметры.
   * @param {string} params.sessionId - ID сессии.
   * @param {string} params.newName - Новое имя.
   */
  updateChatName: ({ sessionId, newName }: { sessionId: string; newName: string }) =>
    apiClient<void>(`/chats/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify({ newName }),
    }),
  /**
   * Получает историю сообщений для указанной сессии.
   * @param {string} sessionId - ID сессии.
   */
  fetchMessages: (sessionId: string): Promise<ServerMessageDto[]> =>
    apiClient<ServerMessageDto[]>(`/chats/${sessionId}/messages`),
  /**
   * Удаляет сессию чата.
   * @param {string} sessionId - ID сессии.
   */
  deleteChatSession: (sessionId: string) => apiClient<void>(`/chats/${sessionId}`, { method: 'DELETE' }),
  /**
   * Обновляет содержимое существующего сообщения.
   * @param {object} params - Параметры.
   * @param {string} params.messageId - ID сообщения.
   * @param {string} params.newContent - Новый текст.
   */
  updateMessage: ({ messageId, newContent }: { messageId: string; newContent: string }) =>
    apiClient<void>(`/messages/${messageId}`, {
      method: 'PUT',
      body: JSON.stringify({ newContent }),
    }),
  /**
   * Удаляет сообщение.
   * @param {string} messageId - ID сообщения.
   */
  deleteMessage: (messageId: string) => apiClient<void>(`/messages/${messageId}`, { method: 'DELETE' }),
  /**
   * Отправляет обратную связь по ответу ассистента.
   * @param {object} params - Параметры.
   * @param {string} params.taskId - ID задачи, сгенерировавшей ответ.
   * @param {boolean} params.isHelpful - Оценка пользователя.
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
   * @param {string} params.childId - ID выбранного дочернего сообщения.
   */
  setActiveBranch: ({ sessionId, parentId, childId }: { sessionId: string; parentId: string; childId: string }) =>
    apiClient<void>(`/chats/${sessionId}/active-branch`, {
        method: 'PUT',
        body: JSON.stringify({ parentMessageId: parentId, activeChildId: childId }),
    }),

  /** Получает список всех файлов для текущего пользователя. */
  getFiles: () => apiClient<FileDto[]>('/files'),
  /**
   * Загружает один файл на сервер.
   * @param {File} file - Объект файла для загрузки.
   */
  uploadFile: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return apiClientMultipart<FileDto>('/files', formData);
  },
  /**
   * Удаляет файл с сервера.
   * @param {string} fileId - ID файла для удаления.
   */
  deleteFile: (fileId: string) => apiClient<void>(`/files/${fileId}`, { method: 'DELETE' }),
};

/**
 * Низкоуровневая сервисная функция для получения потока событий (SSE) от универсального оркестратора.
 * @param {string} query - Текст запроса пользователя.
 * @param {string} sessionId - ID текущей сессии.
 * @param {AbortSignal} signal - Сигнал для отмены запроса.
 * @param {Message[]} history - История сообщений для контекста.
 * @param {string} [context] - Опциональный текстовый контекст (например, содержимое прикрепленного файла).
 * @param {string[]} [fileIds] - Опциональный список ID файлов из файлового менеджера для использования в качестве контекста.
 * @returns {AsyncGenerator<UniversalStreamResponse, void, undefined>} Асинхронный генератор, который отдает события из потока.
 */
export async function* streamChatResponse(
    query: string,
    sessionId: string,
    signal: AbortSignal,
    history: Message[],
    context?: string,
    fileIds?: string[]
): AsyncGenerator<UniversalStreamResponse, void, undefined> {
    const historyDto = history.map(m => ({ type: m.type.toUpperCase(), content: m.text }));
    const response = await fetch('/api/v1/orchestrator/ask-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
        body: JSON.stringify({ query, sessionId, history: historyDto, context, fileIds }),
        signal,
    });

    if (!response.body) throw new Error('Stream body is missing');
    if (!response.ok) {
        const errorText = await response.text();
        let errorJson;
        try {
            errorJson = JSON.parse(errorText);
        } catch {
            errorJson = { detail: errorText };
        }
        throw new Error(`HTTP error! Status: ${response.status}, message: ${errorJson.detail || errorText}`);
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
