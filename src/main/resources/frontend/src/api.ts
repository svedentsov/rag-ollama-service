import { ChatSession, Message } from './types';

const API_BASE_URL = '/api/v1';

async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
    }
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.indexOf("application/json") !== -1) {
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

export const fetchMessages = (sessionId: string): Promise<Message[]> =>
    fetch(`${API_BASE_URL}/chats/${sessionId}/messages`)
        .then(handleResponse<{ role: 'USER' | 'ASSISTANT', content: string }[]>)
        .then(messages =>
            messages.map((msg, index) => ({
                id: `${sessionId}-${index}`, // Генерируем стабильный ID для истории
                type: msg.role === 'USER' ? 'user' : 'assistant',
                text: msg.content,
                sources: [],
            }))
        );

export const deleteChatSession = (sessionId: string): Promise<void> =>
    fetch(`${API_BASE_URL}/chats/${sessionId}`, { method: 'DELETE' }).then(handleResponse);

/**
 * Отправляет оценку (фидбэк) для конкретного RAG-ответа.
 * @param requestId - ID запроса, на который дается оценка.
 * @param isHelpful - Была ли информация полезной.
 * @returns Промис, который разрешается после успешной отправки.
 */
export const sendFeedback = ({ requestId, isHelpful }: { requestId: string; isHelpful: boolean }): Promise<void> =>
    fetch(`${API_BASE_URL}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requestId, isHelpful }),
    }).then(handleResponse);
