import React, { FC, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Message } from '../types';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import styles from './ChatMessage.module.css';

/**
 * @interface MessageContentProps
 * @description Пропсы для компонента MessageContent.
 */
interface MessageContentProps {
  /** @param {Message} message - Объект сообщения для отображения. */
  message: Message;
  /** @param {boolean} isEditing - Находится ли сообщение в режиме редактирования. */
  isEditing: boolean;
  /** @param {(newContent: string) => void} onSave - Колбэк при сохранении изменений. */
  onSave: (newContent: string) => void;
  /** @param {() => void} onCancel - Колбэк при отмене редактирования. */
  onCancel: () => void;
}

/**
 * Презентационный компонент, отвечающий исключительно за отображение
 * содержимого сообщения (текст, Markdown) или редактора.
 * @param {MessageContentProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const MessageContent: FC<MessageContentProps> = ({ message, isEditing, onSave, onCancel }) => {
  const isUser = message.type === 'user';

  /**
   * Мемоизированный объект с компонентами для рендеринга Markdown.
   * Предотвращает их пересоздание на каждый рендер.
   */
  const markdownComponents = useMemo(() => ({
      code: ({ node, className, children, ...props }: any) => {
        const match = /language-(\w+)/.exec(className || '');
        return match
            ? <CodeBlock language={match[1]}>{String(children).replace(/\n$/, '')}</CodeBlock>
            : <code className={styles.inlineCode} {...props}>{children}</code>;
      },
      table: ({ node, ...props }: any) => <table className={styles.markdownTable} {...props} />,
  }), []);

  if (isEditing) {
    return <MessageEditor initialText={message.text} onSave={onSave} onCancel={onCancel} />;
  }

  return (
      <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
        <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
          {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
        </ReactMarkdown>
      </div>
  );
};
