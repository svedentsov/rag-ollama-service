export interface SourceCitation {
    sourceName: string;
    textSnippet: string;
    chunkId: string;
    similarityScore?: number;
    metadata: Record<string, any>;
}

export interface Message {
    id: string;
    type: 'user' | 'assistant';
    text: string;
    sources?: SourceCitation[];
    error?: string;
    rating?: 'up' | 'down';
}

export interface ChatSession {
    sessionId: string;
    chatName: string;
    lastMessageContent?: string;
    lastMessageTimestamp?: string;
}

export type UniversalStreamResponse =
    | { type: 'task_started', taskId: string }
    | { type: 'content', text: string }
    | { type: 'sources', sources: SourceCitation[] }
    | { type: 'done', message: string }
    | { type: 'error', message: string };
