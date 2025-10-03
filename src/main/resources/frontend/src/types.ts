/**
 * Представляет одну цитату источника, использованного в RAG-ответе.
 */
export interface SourceCitation {
  sourceName: string;
  textSnippet: string;
  chunkId: string;
  similarityScore?: number;
  metadata: Record<string, any>;
}

/**
 * Представляет одно сообщение в чате.
 */
export interface Message {
  /** Уникальный идентификатор сообщения (клиентский или серверный). */
  id: string;
  /** Идентификатор задачи, сгенерировавшей это сообщение (если применимо). */
  taskId?: string;
  /** Роль отправителя. */
  type: 'user' | 'assistant';
  /** Текстовое содержимое сообщения. */
  text: string;
  /** ID родительского (пользовательского) сообщения для ответов ассистента. */
  parentId?: string;
  /** Список источников, подтверждающих ответ ассистента. */
  sources?: SourceCitation[];
  /** Текст ошибки, если она произошла при генерации. */
  error?: string;
  /** Флаг, указывающий, что сообщение находится в процессе генерации (стриминга). */
  isStreaming?: boolean;
}

/**
 * Представляет одну сессию чата.
 */
export interface ChatSession {
  sessionId: string;
  chatName: string;
  lastMessageContent?: string;
  lastMessageTimestamp?: string;
}

/**
 * Представляет один из возможных типов событий в потоке SSE от бэкенда.
 */
export type UniversalStreamResponse =
  | { type: 'task_started'; taskId: string }
  | { type: 'status_update'; text: string }
  | { type: 'content'; text: string }
  | { type: 'sources'; sources: SourceCitation[] }
  | { type: 'done'; message: string }
  | { type: 'error'; message: string };