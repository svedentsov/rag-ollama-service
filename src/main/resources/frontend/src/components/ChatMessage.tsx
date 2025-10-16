import React, { FC } from 'react';
import { Message } from '../types';
import { useFeedback } from '../hooks/useFeedback';
import { useChatSessions } from '../hooks/useChatSessions';
import { MessageContent } from './MessageContent';
import { MessageActions } from './MessageActions';
import { BranchControls } from './BranchControls';
import { ThinkingThoughts } from './ThinkingThoughts';
import { Sources } from './Sources';
import { TrustScoreIndicator } from './TrustScoreIndicator';
import { InspectorToolbar } from './InspectorToolbar';
import styles from './ChatMessage.module.css';

/**
 * @interface ChatMessageProps
 * @description Пропсы для компонента ChatMessage, который отображает одно сообщение в чате.
 */
export interface ChatMessageProps {
  /** @param {string} sessionId - ID текущей сессии чата, к которой принадлежит сообщение. */
  sessionId: string;
  /** @param {Message} message - Объект сообщения для отображения. */
  message: Message;
  /** @param {boolean} isLastInTurn - Является ли это сообщение последним в текущем "ходе" диалога (влияет на отображение действий). */
  isLastInTurn: boolean;
  /** @param {{ total: number; current: number; siblings: Message[] }} [branchInfo] - Информация о ветвлении для данного сообщения, если оно имеет альтернативы. */
  branchInfo?: { total: number; current: number; siblings: Message[] };
  /** @param {boolean} isEditing - Находится ли сообщение в режиме редактирования. */
  isEditing: boolean;
  /** @param {() => void} onStartEdit - Колбэк для переключения сообщения в режим редактирования. */
  onStartEdit: () => void;
  /** @param {() => void} onCancelEdit - Колбэк для отмены режима редактирования. */
  onCancelEdit: () => void;
  /** @param {() => void} onRegenerate - Колбэк для запроса повторной генерации ответа ассистента. */
  onRegenerate: () => void;
  /** @param {(messageId: string, newContent: string) => void} onUpdateContent - Колбэк для сохранения измененного контента сообщения. */
  onUpdateContent: (messageId: string, newContent: string) => void;
  /** @param {() => void} onDelete - Колбэк для удаления сообщения и его дочерних веток. */
  onDelete: () => void;
  /** @param {() => void} onStop - Колбэк для остановки потоковой генерации ответа. */
  onStop: () => void;
}

/**
 * Компонент-сборщик (assembler), который отображает одно сообщение в чате.
 * Он объединяет более мелкие, сфокусированные компоненты для отображения контента,
 * метаданных и элементов управления, а также управляет их взаимодействием.
 *
 * @param {ChatMessageProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент сообщения.
 */
export const ChatMessage: FC<ChatMessageProps> = React.memo(({
  sessionId, message, isLastInTurn, branchInfo, isEditing,
  onStartEdit, onCancelEdit, onRegenerate, onUpdateContent, onDelete, onStop,
}) => {
  const { mutate: sendFeedback, isPending: isFeedbackSending } = useFeedback();
  const { setActiveBranch } = useChatSessions();
  const isUser = message.type === 'user';

  const handleSelectBranch = (newChildId: string) => {
    if (message.parentId) {
      setActiveBranch({ sessionId, parentId: message.parentId, childId: newChildId });
    }
  };

  // Если сообщение ассистента находится в процессе стриминга, но еще не имеет текста,
  // отображаем только индикатор "мышления".
  if (message.isStreaming && !message.text) {
    return (
        <div className={`${styles.messageWrapper} ${styles.assistant}`}>
            <ThinkingThoughts assistantMessageId={message.id} />
        </div>
    );
  }

  return (
    <div className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant} ${isEditing ? styles.isEditing : ''}`} tabIndex={-1}>
      <div className={styles.contentWrapper}>
        {/* Панель инспекции отображается только для сообщений ассистента в режиме просмотра */}
        {!isEditing && !isUser && <InspectorToolbar message={message} />}

        {/* Основной контент: Markdown-рендер или редактор */}
        <MessageContent
          message={message}
          isEditing={isEditing}
          onSave={(newContent) => onUpdateContent(message.id, newContent)}
          onCancel={onCancelEdit}
        />

        {/* Дополнительные блоки метаданных для сообщений ассистента */}
        {!isEditing && !isUser && (
          <>
            <Sources message={message} />
            {message.trustScoreReport && <TrustScoreIndicator report={message.trustScoreReport} />}
          </>
        )}

        {/* Нижний блок с элементами управления */}
        <div className={`${styles.messageMetaContainer} ${isLastInTurn || isEditing || branchInfo ? styles.metaVisible : ''}`}>
          <div className={styles.messageActions}>
              {branchInfo && message.parentId && !isEditing && (
                <BranchControls
                  current={branchInfo.current}
                  total={branchInfo.total}
                  onPrev={() => handleSelectBranch(branchInfo.siblings[branchInfo.current - 2].id)}
                  onNext={() => handleSelectBranch(branchInfo.siblings[branchInfo.current].id)}
                />
              )}
              {!isEditing && (
                 <MessageActions
                    message={message}
                    isLastInTurn={isLastInTurn}
                    isFeedbackSending={isFeedbackSending}
                    onRegenerate={onRegenerate}
                    onStartEdit={onStartEdit}
                    onDelete={onDelete}
                    onStop={onStop}
                    onSendFeedback={(isHelpful) => sendFeedback({ taskId: message.taskId!, isHelpful })}
                 />
              )}
          </div>
        </div>
      </div>
    </div>
  );
});
