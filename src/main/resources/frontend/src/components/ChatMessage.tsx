import React, { useState, useMemo, FC, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import toast from 'react-hot-toast';
import { Message } from '../types';
import { RefreshCw, Edit, Trash, ThumbsUp, ThumbsDown, Square, ChevronLeft, ChevronRight } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import { useFeedback } from '../hooks/useFeedback';
import { useClickOutside } from '../hooks/useClickOutside';
import styles from './ChatMessage.module.css';

export interface ChatMessageProps {
  message: Message;
  isLastInTurn: boolean;
  branchInfo?: { total: number; current: number; siblings: Message[] };
  onRegenerate: () => void;
  onUpdateContent: (messageId: string, newContent: string) => void;
  onDelete: (messageId: string) => void;
  onStop: () => void;
  onBranchChange: (parentId: string, newActiveChildId: string) => void;
}

/**
 * Отображает одно сообщение в чате с элегантным встроенным редактированием.
 * @param {ChatMessageProps} props - Пропсы компонента.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  message,
  isLastInTurn,
  branchInfo,
  onRegenerate,
  onUpdateContent,
  onDelete,
  onStop,
  onBranchChange
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const { mutate: sendFeedback, isPending: isFeedbackSending } = useFeedback();
  const isUser = message.type === 'user';

  const editWrapperRef = useRef<HTMLDivElement>(null);
  useClickOutside(editWrapperRef, () => {
    if (isEditing) {
      setIsEditing(false);
    }
  });

  const handleSave = (newContent: string) => {
    onUpdateContent(message.id, newContent);
    setIsEditing(false);
    toast.success('Сообщение обновлено.');
  };

  const handleFeedback = (isHelpful: boolean) => {
    if (message.taskId) {
      sendFeedback({ taskId: message.taskId, isHelpful });
    } else {
      toast.error('Не удалось отправить отзыв: ID задачи отсутствует.');
    }
  };

  const markdownComponents = useMemo(() => ({
      code: ({ node, className, children, ...props }: any) => {
        const match = /language-(\w+)/.exec(className || '');
        return match ? <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock> : <code className={styles.inlineCode} {...props}>{children}</code>;
      },
      table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
  }), []);

  const thinkingIndicator = message.isStreaming && message.text === '';

  if (thinkingIndicator) {
    return (
      <div className={`${styles.messageWrapper} ${styles.assistant}`}>
        <div className={styles.thinkingBubble}><div className={styles.dotFlashing}></div></div>
      </div>
    );
  }

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant}`} tabIndex={-1}>
      <div ref={editWrapperRef} className={styles.contentWrapper}>
        {isEditing ? (
          <MessageEditor
            initialText={message.text}
            onSave={handleSave}
            onCancel={() => setIsEditing(false)}
            saveButtonText="Сохранить"
          />
        ) : (
          <>
            <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
              </ReactMarkdown>
            </div>
            {!isUser && !message.isStreaming && !message.error && (
                <div className={styles.feedbackActions}>
                    <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: true })} disabled={isFeedbackSending} title="Полезный ответ"><ThumbsUp size={14} /></button>
                    <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: false })} disabled={isFeedbackSending} title="Бесполезный ответ"><ThumbsDown size={14} /></button>
                </div>
            )}
            <div className={styles.messageActions}>
              {branchInfo && (
                <div className={styles.branchControls}>
                 <button onClick={() => { if (branchInfo && branchInfo.current > 1 && message.parentId) onBranchChange(message.parentId, branchInfo.siblings[branchInfo.current - 2].id) }} disabled={branchInfo.current <= 1} className={styles.branchButton} aria-label="Предыдущий ответ"><ChevronLeft size={16} /></button>
                 <span className={styles.branchIndicator}>{branchInfo.current} / {branchInfo.total}</span>
                 <button onClick={() => { if (branchInfo && branchInfo.current < branchInfo.total && message.parentId) onBranchChange(message.parentId, branchInfo.siblings[branchInfo.current].id) }} disabled={branchInfo.current >= branchInfo.total} className={styles.branchButton} aria-label="Следующий ответ"><ChevronRight size={16} /></button>
                </div>
              )}
              {!message.isStreaming && (
                <>
                  <button className={styles.actionButton} onClick={() => setIsEditing(true)} title="Редактировать"><Edit size={14} /></button>
                  <button className={styles.actionButton} onClick={() => onDelete(message.id)} title="Удалить"><Trash size={14} /></button>
                </>
              )}
              {!isUser && !message.isStreaming && isLastInTurn && (
                <button className={styles.actionButton} onClick={onRegenerate} title="Повторить генерацию"><RefreshCw size={14} /></button>
              )}
              {message.isStreaming && (
                <button className={styles.actionButton} onClick={onStop} title="Остановить генерацию"><Square size={14} /></button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
});
