import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { v4 as uuidv4 } from 'uuid';
import toast from 'react-hot-toast';
import { useStreamManager } from './useStreamManager';
import { Message } from '../types';
import { useStreamingStore } from '../state/streamingStore';
import { useFileSelectionStore } from '../state/useFileSelectionStore';
import { useFileUpload } from '../features/file-manager/useFileUpload';

/**
 * Хук, инкапсулирующий всю бизнес-логику действий пользователя в чате.
 * @param sessionId - ID текущей сессии чата.
 * @returns Объект с функциями для взаимодействия с чатом и флагами состояния.
 */
export function useChatInteraction(sessionId: string) {
  const queryClient = useQueryClient();
  const { startStream, stopStream } = useStreamManager();
  const { uploadFilesAsync, isUploading } = useFileUpload();
  const { toggleSelection } = useFileSelectionStore();

  const activeStreams = useStreamingStore((state) => state.activeStreams);
  const isStreaming = activeStreams.size > 0;

  const handleSendMessage = useCallback((inputText: string, fileIds?: string[]) => {
    const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText, createdAt: new Date().toISOString(), fileIds };
    const assistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: userMessage.id, isStreaming: true, createdAt: new Date().toISOString() };

    const currentMessages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    queryClient.setQueryData<Message[]>(['messages', sessionId], [...currentMessages, userMessage, assistantMessage]);

    startStream(sessionId, inputText, assistantMessage.id, [...currentMessages, userMessage], fileIds);
  }, [queryClient, sessionId, startStream]);

  const handleRegenerate = useCallback((assistantMessageToRegenerate: Message) => {
    if (!assistantMessageToRegenerate.parentId) return;
    const messages = queryClient.getQueryData<Message[]>(['messages', sessionId]) || [];
    const parentMessageIndex = messages.findIndex(m => m.id === assistantMessageToRegenerate.parentId);

    if (parentMessageIndex > -1) {
        const parentMessage = messages[parentMessageIndex];
        const historyUpToParent = messages.slice(0, parentMessageIndex + 1);
        const newAssistantMessage: Message = { id: uuidv4(), type: 'assistant', text: '', parentId: parentMessage.id, isStreaming: true, createdAt: new Date().toISOString() };

        queryClient.setQueryData<Message[]>(['messages', sessionId], (old = []) => [...old, newAssistantMessage]);
        startStream(sessionId, parentMessage.text, newAssistantMessage.id, historyUpToParent, parentMessage.fileIds);
    }
  }, [queryClient, sessionId, startStream]);

  const handleStopGenerating = useCallback(() => {
    activeStreams.forEach((_, messageId) => stopStream(messageId));
  }, [activeStreams, stopStream]);

  /**
   * Обрабатывает загрузку одного файла и его "прикрепление" к текущей сессии.
   * @param file - Файл для загрузки.
   */
  const handleUploadAndAttach = useCallback((file: File) => {
    toast.promise(
      uploadFilesAsync([file]).then(({ successful }) => {
        if (successful.length > 0) {
          toggleSelection(sessionId, successful[0].id);
        }
      }),
      {
        loading: 'Загрузка файла...',
        success: 'Файл загружен и прикреплен!',
        error: 'Ошибка при загрузке файла.',
      }
    );
  }, [uploadFilesAsync, sessionId, toggleSelection]);

  return {
    handleSendMessage,
    handleRegenerate,
    handleStopGenerating,
    handleUploadAndAttach,
    stopStream,
    isStreaming,
    isUploading,
  };
}
