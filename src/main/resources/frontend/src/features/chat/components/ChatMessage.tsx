import React, { FC } from 'react';
import { Message } from '../../../types';
import { useFeedback } from '../../../hooks/useFeedback';
import { useChatSessions } from '../../../hooks/useChatSessions';
import { MessageContent } from './MessageContent';
import { MessageActions } from './MessageActions';
import { BranchControls } from './BranchControls';
import { ThinkingThoughts } from './ThinkingThoughts'; // <-- ИСПРАВЛЕННЫЙ ИМПОРТ
import { Sources } from './Sources';
import { TrustScoreIndicator } from './TrustScoreIndicator';
import { InspectorToolbar } from './InspectorToolbar';
import styles from './ChatMessage.module.css';

export interface ChatMessageProps {
  sessionId: string;
  message: Message;
  isLastInTurn: boolean;
  branchInfo?: { total: number; current: number; siblings: Message[] };
  isEditing: boolean;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onRegenerate: () => void;
  onUpdateContent: (messageId: string, newContent: string) => void;
  onDelete: () => void;
  onStop: () => void;
}

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
