import React, { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage, ChatMessageProps } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message } from './types';
import { useChatMessages } from './hooks/useChatMessages';
import { useStreamManager } from './hooks/useStreamManager';
import { useScrollManager } from './hooks/useScrollManager';
import styles from './App.module.css';
import toast from "react-hot-toast";

interface AppProps {
  /** @param sessionId - ID текущей сессии чата. */
  sessionId: string;
}

/**
 * Главный компонент-контейнер для чата с полноценной логикой ветвления и удаления.
 * @param {AppProps} props - Пропсы компонента.
 */
const App: React.FC<AppProps> = ({ sessionId }) => {
  const queryClient = useQueryClient();
  const { messages, isLoading: isLoadingHistory, error: historyError, updateMessage, deleteMessage } = useChatMessages(sessionId);
  const { startStream, stopStream } = useStreamManager();
  const [branchSelections, setBranchSelections] = useState<Record<string, string>>({});

  const isAnyMessageStreaming = messages.some(m => m.isStreaming);

  const { visibleMessages, messageBranchInfo } = useMemo(() => {
    if (!messages.length) return { visibleMessages: [], messageBranchInfo: new Map() };
    const childrenMap = new Map<string, Message[]>();
    messages.forEach(m => {
      if (m.parentId) {
        if (!childrenMap.has(m.parentId)) childrenMap.set(m.parentId, []);
        childrenMap.get(m.parentId)!.push(m);
      }
    });
    const hiddenIds = new Set<string>();
    childrenMap.forEach((children, parentId) => {
      if (children.length > 1) {
        let selectedId = branchSelections[parentId];
        if (!selectedId || !children.some(c => c.id === selectedId)) {
          selectedId = children[children.length - 1].id;
        }
        children.forEach(child => {
          if (child.id !== selectedId) hiddenIds.add(child.id);
        });
      }
    });
    const branchInfo = new Map<string, { total: number; current: number; siblings: Message[] }>();
    messages.forEach(msg => {
      if (msg.type === 'assistant' && msg.parentId) {
        const siblings = childrenMap.get(msg.parentId) || [];
        if (siblings.length > 1) {
          const currentIndex = siblings.findIndex(s => s.id === msg.id);
          branchInfo.set(msg.id, { total: siblings.length, current: currentIndex + 1, siblings });
        }
      }
    });
    return { visibleMessages: messages.filter(m => !hiddenIds.has(m.id)), messageBranchInfo: branchInfo };
  }, [messages, branchSelections]);

  const { containerRef, messagesEndRef, showScrollButton, scrollToBottom } = useScrollManager([visibleMessages]);

  const handleSendMessage = (inputText: string) => {
    if (!inputText.trim() || isAnyMessageStreaming) return;
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true };
    queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, userMessage, assistantMessage]);
    startStream(sessionId, inputText, assistantMessage.id);
  };

  /**
   * Обрабатывает запрос на повторную генерацию ответа, реализуя
   * корректное, неразрушающее оптимистичное обновление.
   * @param assistantMessage - Сообщение ассистента, которое нужно перегенерировать.
   */
  const handleRegenerate = (assistantMessage: Message) => {
    if (!assistantMessage.parentId || isAnyMessageStreaming) return;
    const parentMessage = messages.find(m => m.id === assistantMessage.parentId);
    if (parentMessage) {
      const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true };

      // Атомарное оптимистичное обновление: добавляем новое сообщение
      // и сразу же обновляем состояние выбора, чтобы UI показал новую ветку.
      queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
      setBranchSelections(prev => ({ ...prev, [parentMessage.id]: newAssistantMessage.id }));

      startStream(sessionId, parentMessage.text, newAssistantMessage.id);
    }
  };

  const handleUpdateMessage = (messageId: string, newContent: string) => {
      updateMessage({ messageId, newContent });
      toast.success('Сообщение обновлено.');
  };

  const handleDelete = (messageId: string) => {
    const queryKey = ['messages', sessionId];
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

    queryClient.setQueryData<Message[]>(queryKey, (old = []) =>
        old.filter(msg => !idsToDelete.has(msg.id))
    );

    deleteMessage(messageId);
    toast.success('Сообщение удалено.');
  };

  const handleBranchChange = (parentId: string, newActiveChildId: string) => {
    setBranchSelections(prev => ({ ...prev, [parentId]: newActiveChildId }));
  };

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
              message={msg}
              isLastInTurn={isLastMessage(msg.id)}
              branchInfo={messageBranchInfo.get(msg.id)}
              onRegenerate={() => handleRegenerate(msg)}
              onUpdateContent={handleUpdateMessage}
              onDelete={() => handleDelete(msg.id)}
              onStop={() => stopStream(msg.id)}
              onBranchChange={handleBranchChange}
              // Передаем неиспользуемый проп, чтобы удовлетворить интерфейс, но он не будет вызван
              onUpdateAndFork={() => {}}
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
