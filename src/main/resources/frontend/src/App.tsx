import React from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage, ChatMessageProps } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message } from './types';
import { useChatMessages } from './hooks/useChatMessages';
import { useStreamManager } from './hooks/useStreamManager';
import { useScrollManager } from './hooks/useScrollManager';
import styles from './App.module.css';

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
  const { containerRef, messagesEndRef, showScrollButton, scrollToBottom } = useScrollManager([messages]);

  const isAnyMessageStreaming = messages.some(m => m.isStreaming);

  const handleSendMessage = (inputText: string) => {
    if (!inputText.trim() || isAnyMessageStreaming) return;

    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true };

    const queryKey = ['messages', sessionId];
    queryClient.setQueryData<Message[]>(queryKey, (oldData = []) => [...oldData, userMessage, assistantMessage]);

    startStream(sessionId, inputText, assistantMessage.id);
  };

  const handleRegenerate = (assistantMessage: Message) => {
    if (!assistantMessage.parentId || isAnyMessageStreaming) return;
    const parentMessage = messages.find(m => m.id === assistantMessage.parentId);
    if (parentMessage) {
      const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true };
      
      const queryKey = ['messages', sessionId];
      queryClient.setQueryData<Message[]>(queryKey, (oldData = []) =>
        [...oldData.filter(m => m.id !== assistantMessage.id), newAssistantMessage]
      );
      startStream(sessionId, parentMessage.text, newAssistantMessage.id);
    }
  };

  const handleUpdateAndFork = (messageId: string, newContent: string) => {
    if (isAnyMessageStreaming) return;

    const queryKey = ['messages', sessionId];
    const messageIndex = messages.findIndex(m => m.id === messageId);
    if (messageIndex === -1) return;

    updateMessage({ messageId, newContent });

    const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: messageId, isStreaming: true };

    queryClient.setQueryData<Message[]>(queryKey, (oldData = []) => {
        const updatedMessages = oldData.slice(0, messageIndex + 1);
        updatedMessages[messageIndex] = { ...updatedMessages[messageIndex], text: newContent };
        return [...updatedMessages, newAssistantMessage];
    });

    startStream(sessionId, newContent, newAssistantMessage.id);
  };
  
  const findLastTurnMessages = (): { lastUser?: Message, lastAssistant?: Message } => {
    if (!messages || messages.length === 0) return {};
    const lastMessage = messages[messages.length - 1];
    if (lastMessage.type === 'assistant') {
      const parent = messages.find(m => m.id === lastMessage.parentId);
      return { lastUser: parent, lastAssistant: lastMessage };
    }
    return { lastUser: lastMessage };
  };

  const { lastUser, lastAssistant } = findLastTurnMessages();

  return (
    <div className={styles.chatContainer}>
      <div className={styles.chatMessageContainer} ref={containerRef}>
        <div className={styles.messagesWrapper}>
          {isLoadingHistory && <div className={styles.centered}><div className={styles.spinner} role="status" aria-label="Загрузка истории"></div></div>}
          {historyError && <div className={styles.errorAlert}>Не удалось загрузить историю: {(historyError as Error).message}</div>}

          {messages.map((msg) => {
            const isLastInTurn = msg.id === lastUser?.id || msg.id === lastAssistant?.id;
            return (
              <ChatMessage
                key={msg.id}
                message={msg}
                isLastInTurn={isLastInTurn}
                onRegenerate={() => handleRegenerate(msg)}
                onUpdateAndFork={handleUpdateAndFork}
                onDelete={deleteMessage}
                onStop={() => stopStream(msg.id)}
              />
            );
          })}
          <div ref={messagesEndRef} />
        </div>
      </div>
      <ChatInput
        onSendMessage={handleSendMessage}
        isLoading={isAnyMessageStreaming}
        onStopGenerating={() => {
            // Улучшенная логика: находим самое последнее стримящееся сообщение и останавливаем его.
            const streamingMessage = [...messages].reverse().find(m => m.isStreaming);
            if (streamingMessage) {
                stopStream(streamingMessage.id);
            }
        }}
        showScrollButton={showScrollButton}
        onScrollToBottom={() => scrollToBottom('smooth')}
      />
    </div>
  );
};

export default App;