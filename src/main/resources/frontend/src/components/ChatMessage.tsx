import React, { useState, useEffect, useRef, useMemo, FC } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import toast from 'react-hot-toast';
import { Message } from '../types';
import { RefreshCw, Edit, Trash } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import styles from './ChatMessage.module.css';

/**
 * Пропсы для компонента ChatMessage.
 */
interface ChatMessageProps {
  /** Объект сообщения для отображения. */
  message: Message;
  /** Является ли это сообщение последним в списке. */
  isLastMessage: boolean;
  /** Является ли это сообщение последним от пользователя. */
  isLastUserMessage: boolean;
  /** Обработчик для запроса на повторную генерацию ответа. */
  onRegenerate: () => void;
  /** Обработчик для обновления текста сообщения. */
  onUpdate: (messageId: string, newContent: string) => void;
  /** Обработчик для удаления сообщения. */
  onDelete: (messageId: string) => void;
}

/**
 * Отображает одно сообщение в чате, будь то от пользователя или ассистента.
 * Компонент оптимизирован с помощью React.memo для предотвращения ненужных перерисовок.
 * @param {ChatMessageProps} props - Пропсы компонента.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  message,
  isLastMessage,
  isLastUserMessage,
  onRegenerate,
  onUpdate,
  onDelete
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(message.text);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const isUser = message.type === 'user';

  // Автоматически фокусируется на поле ввода и подстраивает его высоту при переходе в режим редактирования.
  useEffect(() => {
    if (isEditing && textareaRef.current) {
      const textarea = textareaRef.current;
      textarea.focus();
      textarea.style.height = 'auto';
      textarea.style.height = `${textarea.scrollHeight}px`;
      textarea.setSelectionRange(textarea.value.length, textarea.value.length);
    }
  }, [isEditing]);

  const handleUpdate = () => {
    if (editText.trim() && editText !== message.text) {
      onUpdate(message.id, editText.trim());
      toast.success('Сообщение обновлено.');
    }
    setIsEditing(false);
  };

  const handleCancel = () => {
    setEditText(message.text);
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleUpdate();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      handleCancel();
    }
  };

  // Мемоизация объекта с компонентами для ReactMarkdown для предотвращения его пересоздания на каждом рендере.
  const markdownComponents = useMemo(() => ({
    code: ({ node, className, children, ...props }: any) => {
      const match = /language-(\w+)/.exec(className || '');
      return match ? (
        <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock>
      ) : (
        <code className={styles.inlineCode} {...props}>{children}</code>
      );
    },
    table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
    p: ({ node, ...props }: any) => <p {...props} />,
  }), []);

  const thinkingIndicator = message.isStreaming && message.text === '';

  if (thinkingIndicator) {
    return (
      <div className={`${styles.messageWrapper} ${styles.assistant}`}>
        <div className={styles.thinkingBubble}>
          <div className={styles.dotFlashing}></div>
        </div>
      </div>
    );
  }

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant}`}>
      <div className={styles.contentWrapper}>
        <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
          {isEditing ? (
            <div className={styles.editContainer}>
              <textarea
                ref={textareaRef}
                value={editText}
                onChange={(e) => setEditText(e.target.value)}
                onKeyDown={handleKeyDown}
                className={styles.editTextarea}
                aria-label="Редактирование сообщения"
              />
              <div className={styles.editActions}>
                <button onClick={handleUpdate} className={styles.saveButton}>Сохранить</button>
                <button onClick={handleCancel} className={styles.cancelButton}>Отмена</button>
              </div>
            </div>
          ) : (
            <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
              {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
            </ReactMarkdown>
          )}
        </div>

        <div className={styles.messageActions}>
          {isUser && isLastUserMessage && !message.isStreaming && !isEditing && (
            <>
              <button className={styles.actionButton} onClick={() => setIsEditing(true)} title="Редактировать" aria-label="Редактировать сообщение">
                <Edit size={14} />
              </button>
              <button className={styles.actionButton} onClick={() => onDelete(message.id)} title="Удалить" aria-label="Удалить сообщение">
                <Trash size={14} />
              </button>
            </>
          )}
          {!isUser && !message.isStreaming && !isEditing && (
             <>
               <button className={styles.actionButton} onClick={() => setIsEditing(true)} title="Редактировать" aria-label="Редактировать ответ">
                 <Edit size={14} />
               </button>
               <button className={styles.actionButton} onClick={() => onDelete(message.id)} title="Удалить" aria-label="Удалить ответ">
                 <Trash size={14} />
               </button>
               {isLastMessage && (
                 <button className={styles.actionButton} onClick={onRegenerate} title="Повторить генерацию" aria-label="Повторить генерацию ответа">
                   <RefreshCw size={14} />
                 </button>
               )}
             </>
          )}
        </div>
      </div>
    </div>
  );
});
