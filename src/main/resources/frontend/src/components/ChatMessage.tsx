import React, { FC, useCallback, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import toast from 'react-hot-toast';
import { Message } from '../types';
import { useChatSessions } from '../hooks/useChatSessions';
import { RefreshCw, Edit, Trash, ThumbsUp, ThumbsDown, Square, ChevronLeft, ChevronRight, Copy } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import { useFeedback } from '../hooks/useFeedback';
import styles from './ChatMessage.module.css';

/**
 * @interface ChatMessageProps
 * @description Пропсы для компонента ChatMessage.
 */
export interface ChatMessageProps {
  /** @param {string} sessionId - ID текущей сессии чата. */
  sessionId: string;
  /** @param {Message} message - Объект сообщения для отображения. */
  message: Message;
  /** @param {boolean} isLastInTurn - Является ли это сообщение последним в текущем "ходе" диалога. */
  isLastInTurn: boolean;
  /** @param {{ total: number; current: number; siblings: Message[] }} [branchInfo] - Информация о ветвлении для данного сообщения. */
  branchInfo?: { total: number; current: number; siblings: Message[] };
  /** @param {boolean} isEditing - Находится ли сообщение в режиме редактирования. */
  isEditing: boolean;
  /** @param {() => void} onStartEdit - Колбэк для начала редактирования. */
  onStartEdit: () => void;
  /** @param {() => void} onCancelEdit - Колбэк для отмены редактирования. */
  onCancelEdit: () => void;
  /** @param {() => void} onRegenerate - Колбэк для запроса повторной генерации ответа. */
  onRegenerate: () => void;
  /** @param {(messageId: string, newContent: string) => void} onUpdateContent - Колбэк для сохранения измененного контента. */
  onUpdateContent: (messageId: string, newContent: string) => void;
  /** @param {() => void} onDelete - Колбэк для удаления сообщения. */
  onDelete: () => void;
  /** @param {() => void} onStop - Колбэк для остановки потоковой генерации. */
  onStop: () => void;
}

/**
 * Отображает одно сообщение в чате, управляя его состоянием и действиями.
 * Компонент оптимизирован с помощью `React.memo` для предотвращения ненужных перерисовок.
 * @param {ChatMessageProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент сообщения.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  sessionId,
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
  const { setActiveBranch } = useChatSessions();
  const isUser = message.type === 'user';
  const iconSize = 18;

  const handleSelectBranch = useCallback((newChildId: string) => {
    if (message.parentId) {
      setActiveBranch({ sessionId, parentId: message.parentId, childId: newChildId });
    }
  }, [sessionId, message.parentId, setActiveBranch]);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(message.text)
      .then(() => toast.success('Скопировано в буфер обмена!'))
      .catch(() => toast.error('Не удалось скопировать.'));
  }, [message.text]);

  const markdownComponents = useMemo(() => ({
      code: ({ node, className, children, ...props }: any) => {
        const match = /language-(\w+)/.exec(className || '');
        return match ? <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock> : <code className={styles.inlineCode} {...props}>{children}</code>;
      },
      table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
  }), []);

  if (message.isStreaming && !message.text) {
    return (
      <div className={`${styles.messageWrapper} ${styles.assistant}`}>
        <div className={styles.thinkingBubble}><div className={styles.dotFlashing}></div></div>
      </div>
    );
  }

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant} ${isEditing ? styles.isEditing : ''}`} tabIndex={-1}>
      <div className={styles.contentWrapper}>
        {isEditing ? (
          <MessageEditor
            initialText={message.text}
            onSave={(newContent) => onUpdateContent(message.id, newContent)}
            onCancel={onCancelEdit}
          />
        ) : (
          <>
            <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
              </ReactMarkdown>
            </div>

            <div className={`${styles.messageActions} ${isLastInTurn ? styles.actionsVisible : ''}`}>
              {branchInfo && message.parentId && (
                <div className={styles.branchControls}>
                 <button onClick={() => handleSelectBranch(branchInfo.siblings[branchInfo.current - 2].id)} disabled={branchInfo.current <= 1} className={styles.branchButton} data-tooltip="Предыдущий ответ" aria-label="Предыдущий ответ"><ChevronLeft size={iconSize} /></button>
                 <span className={styles.branchIndicator}>{branchInfo.current} / {branchInfo.total}</span>
                 <button onClick={() => handleSelectBranch(branchInfo.siblings[branchInfo.current].id)} disabled={branchInfo.current >= branchInfo.total} className={styles.branchButton} data-tooltip="Следующий ответ" aria-label="Следующий ответ"><ChevronRight size={iconSize} /></button>
                </div>
              )}

              {!message.isStreaming ? (
                <>
                  {!isUser && message.taskId && (
                    <>
                      <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: true })} disabled={isFeedbackSending} data-tooltip="Полезный ответ" aria-label="Полезный ответ"><ThumbsUp size={iconSize} /></button>
                      <button className={styles.actionButton} onClick={() => sendFeedback({ taskId: message.taskId!, isHelpful: false })} disabled={isFeedbackSending} data-tooltip="Бесполезный ответ" aria-label="Бесполезный ответ"><ThumbsDown size={iconSize} /></button>
                    </>
                  )}
                  {!isUser && isLastInTurn && (
                    <button className={styles.actionButton} onClick={onRegenerate} data-tooltip="Повторить генерацию" aria-label="Повторить генерацию"><RefreshCw size={iconSize} /></button>
                  )}
                  <button className={styles.actionButton} onClick={handleCopy} data-tooltip="Скопировать" aria-label="Скопировать"><Copy size={iconSize} /></button>
                  <button className={styles.actionButton} onClick={onStartEdit} data-tooltip="Редактировать" aria-label="Редактировать"><Edit size={iconSize} /></button>
                  <button className={styles.actionButton} onClick={onDelete} data-tooltip="Удалить" aria-label="Удалить"><Trash size={iconSize} /></button>
                </>
              ) : (
                <button className={styles.actionButton} onClick={onStop} data-tooltip="Остановить генерацию" aria-label="Остановить генерацию"><Square size={iconSize} /></button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
});
