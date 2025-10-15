import React, { FC, useCallback } from 'react';
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

  const handleSelectBranch = useCallback((newChildId: string) => {
    if (message.parentId) {
      setActiveBranch({ sessionId, parentId: message.parentId, childId: newChildId });
    }
  }, [sessionId, message.parentId, setActiveBranch]);

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
        {!isEditing && !isUser && <InspectorToolbar message={message} />}
        <MessageContent
          message={message}
          isEditing={isEditing}
          onSave={(newContent) => onUpdateContent(message.id, newContent)}
          onCancel={onCancelEdit}
        />
        {!isEditing && !isUser && (
          <>
            <Sources message={message} />
            {message.trustScoreReport && <TrustScoreIndicator report={message.trustScoreReport} />}
          </>
        )}
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
