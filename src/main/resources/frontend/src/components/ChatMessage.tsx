import React, { FC, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Message } from '../types';
import { useBranchSelectionStore } from '../state/branchSelectionStore';
import { RefreshCw, Edit, Trash, ThumbsUp, ThumbsDown, Square, ChevronLeft, ChevronRight } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import { useFeedback } from '../hooks/useFeedback';
import styles from './ChatMessage.module.css';

/**
 * Пропсы для компонента ChatMessage.
 */
export interface ChatMessageProps {
  /** @param message - Объект сообщения для отображения. */
  message: Message;
  /** @param isLastInTurn - Является ли это сообщение последним в текущем "ходе" диалога. */
  isLastInTurn: boolean;
  /** @param branchInfo - Информация о ветвлении, если сообщение является частью ветки. */
  branchInfo?: { total: number; current: number; siblings: Message[] };
  /** @param isEditing - Флаг, указывающий, находится ли сообщение в режиме редактирования. */
  isEditing: boolean;
  /** @param onStartEdit - Колбэк для входа в режим редактирования. */
  onStartEdit: () => void;
  /** @param onCancelEdit - Колбэк для выхода из режима редактирования. */
  onCancelEdit: () => void;
  /** @param onRegenerate - Колбэк для запроса повторной генерации ответа. */
  onRegenerate: () => void;
  /** @param onUpdateContent - Колбэк для сохранения измененного контента. */
  onUpdateContent: (messageId: string, newContent: string) => void;
  /** @param onDelete - Колбэк для удаления сообщения. */
  onDelete: () => void;
  /** @param onStop - Колбэк для остановки потоковой генерации. */
  onStop: () => void;
}

/**
 * Отображает одно сообщение в чате. Компонент теперь напрямую использует
 * стор Zustand для управления ветками, что делает его более автономным.
 * @param {ChatMessageProps} props - Пропсы компонента.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  message,
  isLastInTurn,
  branchInfo,
  isEditing,
  onStartEdit,
  onCancelEdit,
  onRegenerate,
  onUpdateContent,
  onDelete,
  onStop,
}) => {
  const { mutate: sendFeedback, isPending: isFeedbackSending } = useFeedback();
  // Напрямую получаем действие из стора
  const selectBranch = useBranchSelectionStore((state) => state.selectBranch);
  const isUser = message.type === 'user';

  const markdownComponents = useMemo(() => ({
      code: ({ node, className, children, ...props }: any) => {
        const match = /language-(\w+)/.exec(className || '');
        return match ? <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock> : <code className={styles.inlineCode} {...props}>{children}</code>;
      },
      table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
  }), []);

  // Индикатор "Думаю..." для стриминга
  if (message.isStreaming && !message.text) {
    return (
      <div className={`${styles.messageWrapper} ${styles.assistant}`}>
        <div className={styles.thinkingBubble}><div className={styles.dotFlashing}></div></div>
      </div>
    );
  }

  const handleSave = (newContent: string) => {
    onUpdateContent(message.id, newContent);
  };

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant} ${isEditing ? styles.isEditing : ''}`} tabIndex={-1}>
      <div className={styles.contentWrapper}>
        {isEditing ? (
          <MessageEditor
            initialText={message.text}
            onSave={handleSave}
            onCancel={onCancelEdit}
          />
        ) : (
          <>
            <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
              </ReactMarkdown>
            </div>

            <div className={styles.messageActions}>
              {/* Рендерим контролы для ветвления, если есть branchInfo */}
              {branchInfo && message.parentId && (
                <div className={styles.branchControls}>
                 <button onClick={() => selectBranch(message.parentId!, branchInfo.siblings[branchInfo.current - 2].id)} disabled={branchInfo.current <= 1} className={styles.branchButton} aria-label="Предыдущий ответ"><ChevronLeft size={16} /></button>
                 <span className={styles.branchIndicator}>{branchInfo.current} / {branchInfo.total}</span>
                 <button onClick={() => selectBranch(message.parentId!, branchInfo.siblings[branchInfo.current].id)} disabled={branchInfo.current >= branchInfo.total} className={styles.branchButton} aria-label="Следующий ответ"><ChevronRight size={16} /></button>
                </div>
              )}
              {/* Стандартные экшены */}
              {!message.isStreaming && (
                <>
                  <button className={styles.actionButton} onClick={onStartEdit} title="Редактировать"><Edit size={14} /></button>
                  <button className={styles.actionButton} onClick={onDelete} title="Удалить"><Trash size={14} /></button>
                </>
              )}
              {!isUser && !message.isStreaming && isLastInTurn && (
                <button className={styles.actionButton} onClick={onRegenerate} title="Повторить генерацию"><RefreshCw size={14} /></button>
              )}
              {message.isStreaming && (
                <button className={styles.actionButton} onClick={onStop} title="Остановить генерацию"><Square size={14} /></button>
              )}
            </div>

            {/* Блок для фидбэка */}
            {!isUser && !message.isStreaming && !message.error && message.taskId && (
                <div className={styles.feedbackActions}>
                    <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: true })} disabled={isFeedbackSending} title="Полезный ответ"><ThumbsUp size={14} /></button>
                    <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: false })} disabled={isFeedbackSending} title="Бесполезный ответ"><ThumbsDown size={14} /></button>
                </div>
            )}
          </>
        )}
      </div>
    </div>
  );
});
