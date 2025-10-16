import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import { useStreamManager } from './useStreamManager';
import { Message } from '../types';
import { useStreamingStore } from '../state/streamingStore';
import { useAttachmentStore } from '../state/attachmentStore';
import { useFileSelectionStore } from '../state/useFileSelectionStore';

/**
 * @description Хук, инкапсулирующий всю бизнес-логику действий пользователя в чате.
 * @param {string} sessionId - ID текущей сессии чата.
 * @returns {{
 *   handleSendMessage: (inputText: string, context?: string, fileIds?: string[]) => void,
 *   handleRegenerate: (assistantMessageToRegenerate: Message) => void,
 *   handleStopGenerating: () => void,
 *   stopStream: (assistantMessageId: string) => void,
 *   isStreaming: boolean
 * }} Объект с функциями для взаимодействия с чатом и флагом активности.
 */
export function useChatInteraction(sessionId: string) {
  const queryClient = useQueryClient();
  const { startStream, stopStream } = useStreamManager();
  const activeStreams = useStreamingStore((state) => state.activeStreams);
  const isStreaming = activeStreams.size > 0;
  const { clearAttachment } = useAttachmentStore();
  const { clearSelection } = useFileSelectionStore();

  const handleSendMessage = useCallback((inputText: string, context?: string, fileIds?: string[]) => {
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText, createdAt: new Date().toISOString(), context, fileIds };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true, createdAt: new Date().toISOString() };

    const currentMessages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    queryClient.setQueryData<Message[]>(['messages', sessionId], [...currentMessages, userMessage, assistantMessage]);

    startStream(sessionId, inputText, assistantMessage.id, [...currentMessages, userMessage], context, fileIds);

    if (sessionId) {
      if (context) clearAttachment(sessionId);
      if (fileIds && fileIds.length > 0) clearSelection(sessionId);
    }
  }, [queryClient, sessionId, startStream, clearAttachment, clearSelection]);

  const handleRegenerate = useCallback((assistantMessageToRegenerate: Message) => {
    if (!assistantMessageToRegenerate.parentId) return;
    const messages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    const parentMessageIndex = messages.findIndex(m => m.id === assistantMessageToRegenerate.parentId);

    if (parentMessageIndex > -1) {
        const parentMessage = messages[parentMessageIndex];
        const historyUpToParent = messages.slice(0, parentMessageIndex + 1);
        const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true, createdAt: new Date().toISOString() };

        queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
        startStream(sessionId, parentMessage.text, newAssistantMessage.id, historyUpToParent, parentMessage.context, parentMessage.fileIds);
    }
  }, [queryClient, sessionId, startStream]);

  const handleStopGenerating = useCallback(() => {
    activeStreams.forEach((_, messageId) => stopStream(messageId));
  }, [activeStreams, stopStream]);

  return { handleSendMessage, handleRegenerate, handleStopGenerating, stopStream, isStreaming };
}
