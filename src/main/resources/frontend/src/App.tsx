import React, { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message } from './types';
import { useChatMessages } from './hooks/useChatMessages';
import { useStreamManager } from './hooks/useStreamManager';
import { useScrollManager } from './hooks/useScrollManager';
import { useVisibleMessages } from './hooks/useVisibleMessages';
import styles from './App.module.css';

/**
 * Пропсы для компонента App.
 */
interface AppProps {
  /** @param sessionId - ID текущей сессии чата. */
  sessionId: string;
}

/**
 * Главный компонент-контейнер для чата.
 * @param {AppProps} props - Пропсы компонента.
 */
const App: React.FC<AppProps> = ({ sessionId }) => {
  const queryClient = useQueryClient();
  const { messages, isLoading: isLoadingHistory, error: historyError, updateMessage, deleteMessage } = useChatMessages(sessionId);
  const { startStream, stopStream } = useStreamManager();
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);

  const { visibleMessages, messageBranchInfo } = useVisibleMessages(sessionId, messages);

  const { containerRef, messagesEndRef, showScrollButton, scrollToBottom } = useScrollManager([visibleMessages]);
  const isAnyMessageStreaming = messages.some(m => m.isStreaming);

  const handleSendMessage = useCallback((inputText: string) => {
    if (!inputText.trim() || isAnyMessageStreaming) return;
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true };

    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, userMessage, assistantMessage]);
    startStream(sessionId, inputText, assistantMessage.id, sessionId);
  }, [isAnyMessageStreaming, queryClient, sessionId, startStream]);

  const handleRegenerate = useCallback((assistantMessage: Message) => {
    if (!assistantMessage.parentId || isAnyMessageStreaming) return;

    const parentMessage = messages.find(m => m.id === assistantMessage.parentId);
    if (parentMessage) {
      const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true };
      
      queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
      // Здесь больше не нужно вызывать `selectBranch`, так как `useVisibleMessages` 
      // автоматически подхватит новое сообщение как последнее и сделает его видимым.
      // Сохранение выбора произойдет в ChatMessage при явном клике пользователя.

      startStream(sessionId, parentMessage.text, newAssistantMessage.id, sessionId);
    }
  }, [isAnyMessageStreaming, messages, queryClient, sessionId, startStream]);

  const handleUpdateMessage = useCallback((messageId: string, newContent: string) => {
    updateMessage({ messageId, newContent });
    setEditingMessageId(null);
  }, [updateMessage]);

  const handleDeleteMessage = useCallback((messageId: string) => {
    const idsToDelete = new Set<string>([messageId]);
    let newChildrenFound = true;
    while(newChildrenFound) {
        newChildrenFound = false;
        messages.forEach(msg => {
            if (msg.parentId && idsToDelete.has(msg.parentId) && !idsToDelete.has(msg.id)) {
                idsToDelete.add(msg.id);
                newChildrenFound = true;
            }
        });
    }
    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) =>
        old.filter(msg => !idsToDelete.has(msg.id))
    );
    deleteMessage(messageId);
  }, [deleteMessage, messages, queryClient, sessionId]);

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
        isLoading={isAnyMessageStreaming}
        onStopGenerating={() => {
            const streamingMessage = messages.find(m => m.isStreaming);
            if (streamingMessage) stopStream(streamingMessage.id);
        }}
        showScrollButton={showScrollButton}
        onScrollToBottom={() => scrollToBottom('smooth')}
      />
    </div>
  );
};

export default App;
