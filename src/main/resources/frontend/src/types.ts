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
 * @interface ServerMessageDto
 * @description Data Transfer Object (DTO), представляющий структуру сообщения, как она приходит с сервера.
 * Это явный контракт между фронтендом и бэкендом.
 */
export interface ServerMessageDto {
  id: string;
  parentId: string | null;
  role: 'USER' | 'ASSISTANT';
  content: string;
  taskId?: string;
}

/**
 * @interface Message
 * @description Клиентская модель сообщения. Может содержать дополнительные поля для UI-логики,
 * которых нет на сервере (например, `isStreaming`).
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
  activeBranches?: Record<string, string>;
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