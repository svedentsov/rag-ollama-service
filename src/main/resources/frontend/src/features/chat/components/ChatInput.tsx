import React, { useState, useRef, useEffect, FC, FormEvent, KeyboardEvent, ChangeEvent } from 'react';
import toast from 'react-hot-toast';
import { Send, Square, Plus, X, FileText } from 'lucide-react';
import { ScrollToBottomButton } from '../../../components/ScrollToBottomButton';

import styles from './ChatInput.module.css';

/**
 * @interface ChatInputProps
 * @description Пропсы для компонента ChatInput.
 */
interface ChatInputProps {
  /** @param onSendMessage - Колбэк для отправки сообщения. */
  onSendMessage: (text: string, fileIds?: string[]) => void;
  /** @param onStopGenerating - Колбэк для остановки генерации. */
  onStopGenerating: () => void;
  /** @param onUpload - Колбэк для загрузки файла. */
  onUpload: (file: File) => void;
  /** @param isLoading - Флаг, указывающий на процесс генерации ответа. */
  isLoading: boolean;
  /** @param isUploading - Флаг, указывающий на процесс загрузки файла. */
  isUploading: boolean;
  /** @param showScrollButton - Флаг, управляющий видимостью кнопки прокрутки. */
  showScrollButton: boolean;
  /** @param onScrollToBottom - Колбэк для прокрутки к последнему сообщению. */
  onScrollToBottom: () => void;
  /** @param selectedFileIds - Множество ID прикрепленных файлов. */
  selectedFileIds: Set<string>;
  /** @param onClearSelection - Колбэк для очистки выбора файлов. */
  onClearSelection: () => void;
}

const MAX_FILE_SIZE_MB = 10;

/**
 * Презентационный компонент ввода для чата.
 * Делегирует всю сложную логику родительским компонентам через колбэки.
 * @param {ChatInputProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const ChatInput: FC<ChatInputProps> = ({
  onSendMessage, onStopGenerating, onUpload, isLoading, isUploading, showScrollButton, onScrollToBottom, selectedFileIds, onClearSelection
}) => {
  const [inputText, setInputText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isSendDisabled = (!inputText.trim() && selectedFileIds.size === 0) || isLoading || isUploading;

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
      onUpload(file);
    }
    if(event.target) {
      event.target.value = '';
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!isSendDisabled) {
      onSendMessage(inputText, Array.from(selectedFileIds));
      setInputText('');
      onClearSelection();
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !isSendDisabled) {
      e.preventDefault();
      handleSubmit(e as unknown as FormEvent);
    }
  };

  return (
    <div className={styles.chatInputContainer}>
      {showScrollButton && <ScrollToBottomButton onClick={() => onScrollToBottom()} />}
      <form onSubmit={handleSubmit} className={styles.chatInputForm}>
        {selectedFileIds.size > 0 && (
            <div className={styles.attachmentPill}>
                <FileText size={16} />
                <span className={styles.attachmentName}>
                    Прикреплено файлов: {selectedFileIds.size}
                </span>
                <button type="button" className={styles.removeAttachmentButton} onClick={onClearSelection} aria-label="Очистить выбор файлов">
                    <X size={14} />
                </button>
            </div>
        )}
        <div className={styles.inputRow}>
          <input
            type="file" ref={fileInputRef} onChange={handleFileChange}
            accept=".txt,.md,.json,.java,.log,.xml,.yaml,.yml" style={{ display: 'none' }}
            disabled={isLoading || isUploading}
          />
          <button
            type="button" className={styles.iconButton}
            onClick={() => fileInputRef.current?.click()} disabled={isLoading || isUploading}
            aria-label="Прикрепить файл"
          >
            <Plus size={20} />
          </button>
          <textarea
            ref={textareaRef} value={inputText} onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown} placeholder={selectedFileIds.size > 0 ? "Опишите, что сделать с файлом(ами)..." : "Спросите что-нибудь..."}
            className={styles.textarea} rows={1} aria-label="Поле для ввода сообщения"
          />
          <div className={styles.buttonContainer}>
            <button
              type="submit" disabled={isSendDisabled}
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
