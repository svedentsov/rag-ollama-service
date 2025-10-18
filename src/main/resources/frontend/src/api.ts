import { ChatSession, ServerMessageDto, UniversalStreamResponse, Message, FileDto, Page } from './types';

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
 * Обрабатывает базовый fetch и проверяет на ошибки.
 * @param endpoint - Конечная точка API.
 * @param options - Опции для fetch.
 * @returns {Promise<Response>} - Промис с объектом Response.
 */
async function baseApiClient(endpoint: string, options?: RequestInit): Promise<Response> {
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

  return response;
}


/**
 * Выполняет API-запрос и парсит ответ как JSON.
 * @template T - Ожидаемый тип ответа.
 */
async function apiClient<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const response = await baseApiClient(endpoint, options);
  // Если статус No Content, возвращаем undefined, так как тела нет
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

/**
 * Выполняет API-запрос, который не должен возвращать тело ответа (например, PUT, DELETE).
 */
async function apiClientVoid(endpoint: string, options?: RequestInit): Promise<void> {
  await baseApiClient(endpoint, options);
  // Ничего не возвращаем, так как тело ответа не ожидается
}

/**
 * Обрабатывает fetch-запросы, ожидающие текстовый ответ.
 */
async function apiClientText(endpoint: string, options?: RequestInit): Promise<string> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
            'Accept': 'text/plain',
            ...options?.headers,
        },
    });

    if (!response.ok) {
        const errorBody = await response.text();
        throw new ApiError(response.status, { message: errorBody }, `HTTP error on ${endpoint}`);
    }
    return response.text();
}

/**
 * Обрабатывает fetch-запросы multipart/form-data.
 */
async function apiClientMultipart<T>(endpoint: string, formData: FormData): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        body: formData,
    });

    if (!response.ok) {
        const errorBody = await response.json();
        throw new ApiError(response.status, errorBody, `HTTP error on ${endpoint}`);
    }

    return response.json();
}

/**
 * Централизованный обработчик ошибок для всех API-клиентов.
 */
async function handleApi<T>(apiCall: () => Promise<T>): Promise<T> {
    try {
        return await apiCall();
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(503, { message: (error as Error).message }, 'Network or fetch error');
    }
}


/**
 * Объект-фасад, предоставляющий все типизированные методы для взаимодействия с API бэкенда.
 */
export const api = {
  getChatSessions: () => handleApi(() => apiClient<ChatSession[]>('/chats')),
  createNewChat: () => handleApi(() => apiClient<ChatSession>('/chats', { method: 'POST' })),
  updateChatName: (params: { sessionId: string; newName: string }) =>
    handleApi(() => apiClientVoid(`/chats/${params.sessionId}`, {
      method: 'PUT',
      body: JSON.stringify({ newName: params.newName }),
    })),
  fetchMessages: (sessionId: string): Promise<ServerMessageDto[]> =>
    handleApi(() => apiClient<ServerMessageDto[]>(`/chats/${sessionId}/messages`)),
  deleteChatSession: (sessionId: string) => handleApi(() => apiClientVoid(`/chats/${sessionId}`, { method: 'DELETE' })),
  updateMessage: (params: { messageId: string; newContent: string }) =>
    handleApi(() => apiClientVoid(`/messages/${params.messageId}`, {
      method: 'PUT',
      body: JSON.stringify({ newContent: params.newContent }),
    })),
  deleteMessage: (messageId: string) => handleApi(() => apiClientVoid(`/messages/${messageId}`, { method: 'DELETE' })),
  sendFeedback: (params: { taskId: string; isHelpful: boolean }) =>
    handleApi(() => apiClientVoid('/feedback', {
      method: 'POST',
      body: JSON.stringify({ requestId: params.taskId, isHelpful: params.isHelpful }),
    })),
  setActiveBranch: (params: { sessionId: string; parentId: string; childId: string }) =>
    handleApi(() => apiClientVoid(`/chats/${params.sessionId}/active-branch`, {
        method: 'PUT',
        body: JSON.stringify({ parentMessageId: params.parentId, activeChildId: params.childId }),
    })),

  getFiles: (params: { page: number; size: number; sort: string; direction: string; query: string }) => {
    const queryParams = new URLSearchParams({
      page: params.page.toString(),
      size: params.size.toString(),
      sort: params.sort,
      direction: params.direction,
      query: params.query,
    });
    return handleApi(() => apiClient<Page<FileDto>>(`/files?${queryParams.toString()}`));
  },
  uploadFile: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return handleApi(() => apiClientMultipart<FileDto>('/files', formData));
  },
  deleteFiles: (fileIds: string[]) => handleApi(() => apiClientVoid(`/files`, {
    method: 'DELETE',
    body: JSON.stringify(fileIds)
  })),
  getFileContent: (fileId: string) => handleApi(() => apiClientText(`/files/${fileId}/content`)),
};

/**
 * Низкоуровневая сервисная функция для получения потока событий (SSE).
 */
export async function* streamChatResponse(
    query: string,
    sessionId: string,
    signal: AbortSignal,
    history: Message[],
    fileIds?: string[]
): AsyncGenerator<UniversalStreamResponse, void, undefined> {
    const historyDto = history.map(m => ({ type: m.type.toUpperCase(), content: m.text }));
    const response = await fetch('/api/v1/orchestrator/ask-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
        body: JSON.stringify({ query, sessionId, history: historyDto, fileIds }),
        signal,
    });

    if (!response.body) throw new Error('Stream body is missing');
    if (!response.ok) {
        const errorText = await response.text();
        let errorJson;
        try { errorJson = JSON.parse(errorText); } catch { errorJson = { detail: errorText }; }
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
