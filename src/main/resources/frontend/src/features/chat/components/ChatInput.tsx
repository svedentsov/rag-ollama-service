import React, { useState, useRef, useEffect, FC, FormEvent, KeyboardEvent, ChangeEvent } from 'react';
import toast from 'react-hot-toast';
import { Send, Square, Plus, X, FileText } from 'lucide-react';
import { ScrollToBottomButton } from '../../../components/ScrollToBottomButton';
import { useAttachmentStore } from '../../../state/attachmentStore';
import { useSessionStore } from '../../../state/sessionStore';
// ИСПРАВЛЕНИЕ: Исправлено имя файла
import { useFileSelectionStore } from '../../../state/useFileSelectionStore';
import styles from './ChatInput.module.css';

interface ChatInputProps {
  onSendMessage: (text: string, context?: string, fileIds?: string[]) => void;
  onStopGenerating: () => void;
  isLoading: boolean;
  showScrollButton: boolean;
  onScrollToBottom: () => void;
}

const MAX_FILE_SIZE_MB = 5;

export const ChatInput: FC<ChatInputProps> = ({
  onSendMessage, onStopGenerating, isLoading, showScrollButton, onScrollToBottom
}) => {
  const [inputText, setInputText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const sessionId = useSessionStore((state) => state.currentSessionId);
  const { attachment, setAttachment, clearAttachment } = useAttachmentStore((state) => ({
    attachment: state.attachments.get(sessionId || '') || null,
    setAttachment: (file: { name: string; content: string }) => sessionId && state.setAttachment(sessionId, file),
    clearAttachment: () => sessionId && state.clearAttachment(sessionId),
  }));
  const { getSelectedIds, clearSelection } = useFileSelectionStore();
  const selectedFileIds = sessionId ? getSelectedIds(sessionId) : new Set();

  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${textarea.scrollHeight}px`;
    }
  }, [inputText]);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
        toast.error(`Файл слишком большой. Максимальный размер: ${MAX_FILE_SIZE_MB} МБ.`);
        return;
      }
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        setAttachment({ name: file.name, content });
      };
      reader.onerror = () => { toast.error('Не удалось прочитать файл.'); };
      reader.readAsText(file);
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if ((inputText.trim() || attachment || selectedFileIds.size > 0) && !isLoading) {
      onSendMessage(inputText, attachment?.content, Array.from(selectedFileIds));
      setInputText('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !isLoading) {
      e.preventDefault();
      handleSubmit(e as unknown as FormEvent);
    }
  };

  return (
    <div className={styles.chatInputContainer}>
      {showScrollButton && <ScrollToBottomButton onClick={() => onScrollToBottom()} />}
      <form onSubmit={handleSubmit} className={styles.chatInputForm}>
        {attachment && (
          <div className={styles.attachmentPill}>
            <FileText size={16} />
            <span className={styles.attachmentName}>{attachment.name}</span>
            <button type="button" className={styles.removeAttachmentButton} onClick={clearAttachment} aria-label="Удалить прикрепленный файл">
              <X size={14} />
            </button>
          </div>
        )}
        {selectedFileIds.size > 0 && !attachment && (
            <div className={styles.attachmentPill}>
                <FileText size={16} />
                <span className={styles.attachmentName}>
                    Прикреплено файлов: {selectedFileIds.size}
                </span>
                <button type="button" className={styles.removeAttachmentButton} onClick={() => sessionId && clearSelection(sessionId)} aria-label="Очистить выбор файлов">
                    <X size={14} />
                </button>
            </div>
        )}
        <div className={styles.inputRow}>
          <input
            type="file" ref={fileInputRef} onChange={handleFileChange}
            accept=".txt,.md,.json,.java,.log,.xml,.yaml,.yml" style={{ display: 'none' }}
          />
          <button
            type="button" className={styles.iconButton}
            onClick={() => fileInputRef.current?.click()} disabled={!!attachment || selectedFileIds.size > 0}
            aria-label="Прикрепить файл"
          >
            <Plus size={20} />
          </button>
          <textarea
            ref={textareaRef} value={inputText} onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown} placeholder={attachment || selectedFileIds.size > 0 ? "Опишите, что сделать с файлом(ами)..." : "Спросите что-нибудь..."}
            className={styles.textarea} rows={1} aria-label="Поле для ввода сообщения"
          />
          <div className={styles.buttonContainer}>
            <button
              type="submit" disabled={!inputText.trim() && !isLoading && !attachment && selectedFileIds.size === 0}
              className={`${styles.sendButton} ${!isLoading ? styles.visible : styles.hidden}`}
              aria-label="Отправить сообщение"
            >
              <Send size={20} />
            </button>
            <button
              type="button" onClick={onStopGenerating}
              className={`${styles.stopButton} ${isLoading ? styles.visible : styles.hidden}`}
              aria-label="Остановить генерацию"
            >
              <Square size={20} />
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
