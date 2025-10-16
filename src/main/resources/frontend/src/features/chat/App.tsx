import React, { useState, useCallback } from 'react';
import { useChatMessages } from '../../hooks/useChatMessages';
import { useScrollManager } from '../../hooks/useScrollManager';
import { useVisibleMessages } from '../../hooks/useVisibleMessages';
import { useChatInteraction } from '../../hooks/useChatInteraction';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import styles from './App.module.css';

/**
 * @interface AppProps
 * @description Пропсы для корневого компонента приложения чата.
 */
interface AppProps {
  /** @param {string} sessionId - ID текущей активной сессии чата. */
  sessionId: string;
}

/**
 * Главный компонент-контейнер для чата.
 * @param {AppProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент чата.
 */
export const App: React.FC<AppProps> = ({ sessionId }) => {
  const { messages, isLoading: isLoadingHistory, error: historyError, updateMessage, deleteMessage } = useChatMessages(sessionId);
  const { handleSendMessage, handleRegenerate, handleStopGenerating, stopStream, isStreaming } = useChatInteraction(sessionId);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);

  const { visibleMessages, messageBranchInfo } = useVisibleMessages(sessionId, messages);
  const { containerRef, messagesEndRef, showScrollButton, scrollToBottom } = useScrollManager([visibleMessages]);

  const handleUpdateMessage = useCallback((messageId: string, newContent: string) => {
    updateMessage({ messageId, newContent });
    setEditingMessageId(null);
  }, [updateMessage]);

  const handleDeleteMessage = useCallback((messageId: string) => {
    deleteMessage(messageId);
  }, [deleteMessage]);

  const isLastMessage = (msgId: string) => visibleMessages.length > 0 && visibleMessages[visibleMessages.length - 1].id === msgId;

  return (
    <div className={styles.chatContainer}>
      <div className={styles.chatMessageContainer} ref={containerRef}>
        <div className={styles.messagesWrapper}>
          {isLoadingHistory && <div className={styles.centered}><div className={styles.spinner} role="status" aria-label="Загрузка истории"></div></div>}
          {historyError && <div className={styles.errorAlert}>Не удалось загрузить историю: {(historyError as Error).message}</div>}

          {visibleMessages.map((msg) => (
            <ChatMessage
              key={msg.id}
              sessionId={sessionId}
              message={msg}
              isLastInTurn={isLastMessage(msg.id)}
              branchInfo={messageBranchInfo.get(msg.id)}
              isEditing={editingMessageId === msg.id}
              onStartEdit={() => setEditingMessageId(msg.id)}
              onCancelEdit={() => setEditingMessageId(null)}
              onRegenerate={() => handleRegenerate(msg)}
              onUpdateContent={handleUpdateMessage}
              onDelete={() => handleDeleteMessage(msg.id)}
              onStop={() => stopStream(msg.id)}
            />
          ))}
          <div ref={messagesEndRef} />
        </div>
      </div>
      <ChatInput
        onSendMessage={handleSendMessage}
        isLoading={isStreaming}
        onStopGenerating={handleStopGenerating}
        showScrollButton={showScrollButton}
        onScrollToBottom={() => scrollToBottom('smooth')}
      />
    </div>
  );
};
