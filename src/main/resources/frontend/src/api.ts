import { ChatSession, Message } from './types';

const API_BASE_URL = '/api/v1';

export async function fetchChatSessions(): Promise<ChatSession[]> {
    const response = await fetch(`${API_BASE_URL}/chats`);
    if (!response.ok) {
        throw new Error('Failed to fetch chat sessions');
    }
    return response.json();
}

export async function createNewChat(): Promise<ChatSession> {
    const response = await fetch(`${API_BASE_URL}/chats`, {
        method: 'POST',
    });
    if (!response.ok) {
        throw new Error('Failed to create new chat');
    }
    return response.json();
}

export async function updateChatName(sessionId: string, newName: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/chats/${sessionId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ newName }),
    });
    if (!response.ok) {
        throw new Error(`Failed to update chat name for session ${sessionId}`);
    }
}

export async function fetchMessages(sessionId: string): Promise<Message[]> {
    const response = await fetch(`${API_BASE_URL}/chats/${sessionId}/messages`);
    if (!response.ok) {
        throw new Error(`Failed to fetch messages for session ${sessionId}`);
    }
    const messages: { role: 'USER' | 'ASSISTANT', content: string }[] = await response.json();

    return messages.map(msg => ({
        type: msg.role === 'USER' ? 'user' : 'assistant',
        text: msg.content,
        sources: []
    }));
}

export async function deleteChatSession(sessionId: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/chats/${sessionId}`, {
        method: 'DELETE',
    });
    if (!response.ok) {
        throw new Error(`Failed to delete chat session ${sessionId}`);
    }
}
