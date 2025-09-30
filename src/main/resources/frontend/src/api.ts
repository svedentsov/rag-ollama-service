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
        .then(handleResponse<{ id: string, role: 'USER' | 'ASSISTANT', content: string }[]>)
        .then(messages =>
            messages.map(msg => ({
                id: msg.id,
                type: msg.role === 'USER' ? 'user' : 'assistant',
                text: msg.content,
                sources: [],
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

export const sendFeedback = ({ requestId, isHelpful }: { requestId: string; isHelpful: boolean }): Promise<void> =>
    fetch(`${API_BASE_URL}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requestId, isHelpful }),
    }).then(handleResponse);
