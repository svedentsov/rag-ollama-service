import React, { useState, useMemo, FC } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import toast from 'react-hot-toast';
import { Message } from '../types';
import { RefreshCw, Edit, Trash, ThumbsUp, ThumbsDown, Square } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import { useFeedback } from '../hooks/useFeedback';
import styles from './ChatMessage.module.css';

export interface ChatMessageProps {
  /** @param message - Объект сообщения для отображения. */
  message: Message;
  /** @param isLastInTurn - Является ли это сообщение последним в паре "вопрос-ответ". */
  isLastInTurn: boolean;
  /** @param onRegenerate - Обработчик для запроса на повторную генерацию ответа. */
  onRegenerate: () => void;
  /** @param onUpdateAndFork - Обработчик для обновления сообщения с созданием новой ветки диалога. */
  onUpdateAndFork: (messageId: string, newContent: string) => void;
  /** @param onDelete - Обработчик для удаления сообщения. */
  onDelete: (messageId: string) => void;
  /** @param onStop - Обработчик для остановки генерации этого сообщения. */
  onStop: () => void;
}

/**
 * Отображает одно сообщение в чате.
 * @param {ChatMessageProps} props - Пропсы компонента.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  message,
  isLastInTurn,
  onRegenerate,
  onUpdateAndFork,
  onDelete,
  onStop
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const { mutate: sendFeedback, isPending: isFeedbackSending } = useFeedback();
  const isUser = message.type === 'user';

  const handleUpdate = (newContent: string) => {
    onUpdateAndFork(message.id, newContent);
    setIsEditing(false);
    toast.success('Запрос обновлен. Генерируется новый ответ...');
  };

  const handleFeedback = (isHelpful: boolean) => {
    if (message.taskId) {
      sendFeedback({ taskId: message.taskId, isHelpful });
    } else {
      toast.error('Не удалось отправить отзыв: ID задачи отсутствует.');
    }
  };

  // Логика рендеринга Markdown возвращена в компонент, которому она принадлежит.
  const markdownComponents = useMemo(() => ({
      code: ({ node, className, children, ...props }: any) => {
        const match = /language-(\w+)/.exec(className || '');
        return match ? <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock> : <code className={styles.inlineCode} {...props}>{children}</code>;
      },
      table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
  }), []);

  const thinkingIndicator = message.isStreaming && message.text === '' && !message.error;

  if (thinkingIndicator) {
    return (
      <div className={`${styles.messageWrapper} ${styles.assistant}`}>
        <div className={styles.thinkingBubble}><div className={styles.dotFlashing}></div></div>
      </div>
    );
  }

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant}`} tabIndex={-1}>
      <div className={styles.contentWrapper}>
        <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
          {isEditing ? (
            <MessageEditor
              initialText={message.text}
              onSave={handleUpdate}
              onCancel={() => setIsEditing(false)}
            />
          ) : (
            <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
              {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
            </ReactMarkdown>
          )}
        </div>

        {!isUser && !message.isStreaming && !message.error && (
            <div className={styles.feedbackActions}>
                <button className={styles.actionButton} onClick={() => handleFeedback(true)} disabled={isFeedbackSending} title="Полезный ответ"><ThumbsUp size={14} /></button>
                <button className={styles.actionButton} onClick={() => handleFeedback(false)} disabled={isFeedbackSending} title="Бесполезный ответ"><ThumbsDown size={14} /></button>
            </div>
        )}

        <div className={styles.messageActions}>
          {isUser && isLastInTurn && !message.isStreaming && !isEditing && (
            <>
              <button className={styles.actionButton} onClick={() => setIsEditing(true)} title="Редактировать и отправить заново"><Edit size={14} /></button>
              <button className={styles.actionButton} onClick={() => onDelete(message.id)} title="Удалить"><Trash size={14} /></button>
            </>
          )}
          {!isUser && !isEditing && (
            <>
              {message.isStreaming ? (
                <button className={styles.actionButton} onClick={onStop} title="Остановить генерацию"><Square size={14} /></button>
              ) : (
                isLastInTurn && <button className={styles.actionButton} onClick={onRegenerate} title="Повторить генерацию"><RefreshCw size={14} /></button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
});
