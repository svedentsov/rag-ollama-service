import React, { useState, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message } from './types';
import { useChatMessages } from './hooks/useChatMessages';
import { useChatStream } from './hooks/useChatStream';
import { useScrollManager } from './hooks/useScrollManager';
import { StatusIndicator } from "./components/StatusIndicator";
import styles from './App.module.css';

interface AppProps {
  sessionId: string;
}

/**
 * Главный компонент-контейнер для чата.
 * Отвечает за оркестрацию дочерних компонентов и управление потоками данных,
 * делегируя сложную логику специализированным хукам.
 * @param {AppProps} props - Пропсы компонента.
 */
const App: React.FC<AppProps> = ({ sessionId }) => {
  const queryClient = useQueryClient();
  const { messages, isLoading: isLoadingHistory, error: historyError, updateMessage, deleteMessage } = useChatMessages(sessionId);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [currentStatusText, setCurrentStatusText] = useState<string | null>(null);

  const { containerRef, messagesEndRef, showScrollButton, scrollToBottom } = useScrollManager([messages]);

  const { mutate: streamRequest, isPending: isThinking, stop } = useChatStream({
    sessionId,
    queryClient,
    onStreamEnd: () => {
      setCurrentStatusText(null);
      // Финальная сверка с сервером для консистентности
      queryClient.invalidateQueries({ queryKey: ['messages', sessionId] });
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    },
  });

  // Таймер для индикатора загрузки
  useEffect(() => {
    let timerInterval: number | undefined;
    if (isThinking) {
      setCurrentStatusText("Думаю..."); // Начальный статус
      timerInterval = window.setInterval(() => {
        setElapsedTime(prev => prev + 1);
      }, 1000);
    } else {
      clearInterval(timerInterval);
      setElapsedTime(0);
    }
    return () => clearInterval(timerInterval);
  }, [isThinking]);

  const handleSendMessage = (inputText: string) => {
    if (!inputText.trim()) return;

    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText, sources: [] };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', sources: [], parentId: userMessage.id, isStreaming: true };

    // Оптимистичное обновление кэша для мгновенного отображения
    const queryKey = ['messages', sessionId];
    queryClient.setQueryData<Message[]>(queryKey, (oldData = []) => [...oldData, userMessage, assistantMessage]);

    streamRequest({ query: inputText, assistantMessageId: assistantMessage.id });
  };

  const handleRegenerate = (assistantMessage: Message) => {
    if (!assistantMessage.parentId) return;
    const parentMessage = messages.find(m => m.id === assistantMessage.parentId);
    if (parentMessage) {
      // Удаляем старый ответ ассистента и создаем новый
      const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', sources: [], parentId: parentMessage.id, isStreaming: true };

      const queryKey = ['messages', sessionId];
      queryClient.setQueryData<Message[]>(queryKey, (oldData = []) =>
        [...oldData.filter(m => m.id !== assistantMessage.id), newAssistantMessage]
      );
      streamRequest({ query: parentMessage.text, assistantMessageId: newAssistantMessage.id });
    }
  };

  const lastUserMessage = [...messages].reverse().find(m => m.type === 'user');

  return (
    <div className={styles.chatContainer}>
      <div className={styles.chatMessageContainer} ref={containerRef}>
        <div className={styles.messagesWrapper}>
          {isLoadingHistory && <div className={styles.centered}><div className={styles.spinner} role="status" aria-label="Загрузка истории"></div></div>}
          {historyError && <div className={styles.errorAlert}>Не удалось загрузить историю: {(historyError as Error).message}</div>}

          {messages.map((msg, index) => (
            <ChatMessage
              key={msg.id}
              message={msg}
              isLastMessage={index === messages.length - 1}
              isLastUserMessage={msg.type === 'user' && msg.id === lastUserMessage?.id}
              onRegenerate={() => handleRegenerate(msg)}
              onUpdate={(messageId, newContent) => updateMessage({ messageId, newContent })}
              onDelete={deleteMessage}
            />
          ))}

          {isThinking && (
            <StatusIndicator
              statusText={currentStatusText || "Думаю..."}
              elapsedTime={elapsedTime}
            />
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>
      <ChatInput
        onSendMessage={handleSendMessage}
        onStopGenerating={stop}
        isLoading={isThinking}
        showScrollButton={showScrollButton}
        onScrollToBottom={() => scrollToBottom('smooth')}
      />
    </div>
  );
};

export default App;