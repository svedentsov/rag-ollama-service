import { ChatSession, Message, UniversalStreamResponse } from './types';

const API_BASE_URL = '/api/v1';

/**
 * Универсальный обработчик ответов от fetch.
 * @param response - Объект Response.
 * @returns Промис с распарсенным JSON или undefined.
 * @throws {Error} Если ответ не 'ok'.
 */
async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
    }
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return response.json();
    }
    return undefined as T;
}

export const fetchChatSessions = (): Promise<ChatSession[]> =>
    fetch(`${API_BASE_URL}/chats`).then(handleResponse);

export const createNewChat = (): Promise<ChatSession> =>
    fetch(`${API_BASE_URL}/chats`, { method: 'POST' }).then(handleResponse);

export const updateChatName = ({ sessionId, newName }: { sessionId: string; newName: string }): Promise<void> =>
    fetch(`${API_BASE_URL}/chats/${sessionId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newName }),
    }).then(handleResponse);

/**
 * Запрашивает историю сообщений для сессии.
 * Эта функция исправлена для корректной обработки полной структуры DTO с бэкенда.
 * @param sessionId - ID сессии.
 * @returns Промис с массивом сообщений.
 */
export const fetchMessages = (sessionId: string): Promise<Message[]> =>
    fetch(`${API_BASE_URL}/chats/${sessionId}/messages`)
        .then(handleResponse<{ id: string; parentId: string | null; role: 'USER' | 'ASSISTANT'; content: string; taskId?: string }[]>)
        .then(messages =>
            messages.map(msg => ({
                id: msg.id,
                taskId: msg.taskId,
                parentId: msg.parentId ?? undefined,
                type: msg.role === 'USER' ? 'user' : 'assistant',
                text: msg.content,
                sources: [], // `sources` приходят только через SSE, при загрузке истории они пусты
                isStreaming: false,
            }))
        );


export const deleteChatSession = (sessionId: string): Promise<void> =>
    fetch(`${API_BASE_URL}/chats/${sessionId}`, { method: 'DELETE' }).then(handleResponse);

export const updateMessage = ({ messageId, newContent }: { messageId: string; newContent: string }): Promise<void> =>
    fetch(`${API_BASE_URL}/messages/${messageId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newContent }),
    }).then(handleResponse);

export const deleteMessage = (messageId: string): Promise<void> =>
    fetch(`${API_BASE_URL}/messages/${messageId}`, { method: 'DELETE' }).then(handleResponse);

export const sendFeedback = ({ taskId, isHelpful }: { taskId: string; isHelpful: boolean }): Promise<void> =>
    fetch(`${API_BASE_URL}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requestId: taskId, isHelpful }),
    }).then(handleResponse);

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
        headers: { 'Content-Type': 'application/json' },
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
                    console.error('Failed to parse SSE data:', jsonData, e);
                }
            }
        }
    }
}
