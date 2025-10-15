/**
 * DTO для отчета об оценке доверия к RAG-ответу.
 */
export interface TrustScoreReport {
  finalScore: number;
  confidenceScore: number;
  recencyScore: number;
  authorityScore: number;
  justification: string;
}

/**
 * Представляет один шаг в процессе формирования запроса.
 */
export interface QueryFormationStep {
  stepName: string;
  description: string;
  result: string | string[];
}

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
 * Представляет один шаг в процессе "мышления" AI.
 */
export interface ThinkingStep {
    name: string;
    status: 'RUNNING' | 'COMPLETED';
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
  createdAt: string;
  taskId?: string;
  sourceCitations?: SourceCitation[];
  queryFormationHistory?: QueryFormationStep[];
  finalPrompt?: string;
  trustScoreReport?: TrustScoreReport;
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
  /** Временная метка создания сообщения. */
  createdAt: string;
  /** ID родительского (пользовательского) сообщения для ответов ассистента. */
  parentId?: string;
  /** Список источников, подтверждающих ответ ассистента. */
  sources?: SourceCitation[];
  /** История трансформации исходного запроса. */
  queryFormationHistory?: QueryFormationStep[];
  /** Полный текст финального промпта, отправленного в LLM. */
  finalPrompt?: string;
  /** Отчет об оценке доверия к ответу. */
  trustScoreReport?: TrustScoreReport;
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
  | { type: 'thinking_thought'; stepName: string; status: 'RUNNING' | 'COMPLETED' }
  | { type: 'content'; text: string }
  | { type: 'sources'; sources: SourceCitation[], queryFormationHistory?: QueryFormationStep[], finalPrompt?: string, trustScoreReport?: TrustScoreReport }
  | { type: 'code', generatedCode: string, language: string, finalPrompt?: string }
  | { type: 'done'; message: string }
  | { type: 'error'; message: string };