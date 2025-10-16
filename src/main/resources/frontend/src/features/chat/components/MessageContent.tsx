import React, { FC, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Message } from '../types';
import { CodeBlock } from './CodeBlock';
import { MessageEditor } from './MessageEditor';
import styles from './ChatMessage.module.css';

/**
 * @interface MessageContentProps
 * @description Пропсы для компонента, отображающего содержимое сообщения.
 */
interface MessageContentProps {
  /** @param {Message} message - Объект сообщения для отображения. */
  message: Message;
  /** @param {boolean} isEditing - Флаг, указывающий, находится ли сообщение в режиме редактирования. */
  isEditing: boolean;
  /** @param {(newContent: string) => void} onSave - Колбэк для сохранения измененного контента. */
  onSave: (newContent: string) => void;
  /** @param {() => void} onCancel - Колбэк для отмены редактирования. */
  onCancel: () => void;
}

/**
 * Презентационный компонент, отвечающий исключительно за отображение
 * содержимого сообщения (текст, отформатированный Markdown) или редактора для его изменения.
 * Является "глупым" компонентом, вся логика передается через пропсы.
 *
 * @param {MessageContentProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const MessageContent: FC<MessageContentProps> = ({ message, isEditing, onSave, onCancel }) => {
  const isUser = message.type === 'user';

  /**
   * Мемоизированный объект с кастомными рендерерами для ReactMarkdown.
   * Предотвращает их пересоздание на каждый рендер, оптимизируя производительность.
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

  const contentBubbleClass = isUser ? styles.userBubble : styles.assistantBubble;
  const errorClass = message.error ? styles.isError : '';

  return (
      <div className={`${styles.bubble} ${contentBubbleClass} ${errorClass}`}>
        <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
          {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
        </ReactMarkdown>
      </div>
  );
};
