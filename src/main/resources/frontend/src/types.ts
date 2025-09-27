/**
 * Описывает одну структурированную цитату (источник), использованную в RAG-ответе.
 */
export interface SourceCitation {
    sourceName: string;
    textSnippet: string;
    chunkId: string;
    similarityScore?: number;
    metadata: Record<string, any>;
}

/**
 * Описывает одно сообщение в чате.
 * Добавлен ID для уникальной идентификации, а также поля для редактирования и оценки.
 */
export interface Message {
    id: string; // Уникальный ID, генерируемый на клиенте
    type: 'user' | 'assistant';
    text: string;
    sources?: SourceCitation[];
    error?: string;
    rating?: 'up' | 'down'; // Состояние оценки
}

/**
 * Описывает метаданные одной сессии чата.
 */
export interface ChatSession {
    sessionId: string;
    chatName: string;
    lastMessageContent?: string;
    lastMessageTimestamp?: string;
}

/**
 * Описывает полиморфный объект ответа, приходящий в потоке Server-Sent Events.
 */
export type UniversalStreamResponse =
    | { type: 'task_started', taskId: string }
    | { type: 'content', text: string }
    | { type: 'sources', sources: SourceCitation[] }
    | { type: 'done', message: string }
    | { type: 'error', message: string };
