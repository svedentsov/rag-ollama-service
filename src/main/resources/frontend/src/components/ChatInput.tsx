import React, { useState, useRef, useEffect, FC, FormEvent, KeyboardEvent } from 'react';
import { Send, Square } from 'lucide-react';
import { ScrollToBottomButton } from "./ScrollToBottomButton";
import styles from './ChatInput.module.css';

/**
 * Пропсы для компонента ChatInput.
 * @interface
 */
interface ChatInputProps {
  /** Колбэк для отправки нового сообщения. */
  onSendMessage: (text: string) => void;
  /** Колбэк для остановки всех активных генераций. */
  onStopGenerating: () => void;
  /** Флаг, указывающий, активен ли хотя бы один процесс генерации. */
  isLoading: boolean;
  /** Флаг для отображения кнопки прокрутки вниз. */
  showScrollButton: boolean;
  /** Колбэк для плавной прокрутки вниз. */
  onScrollToBottom: () => void;
}

/**
 * Компонент для ввода и отправки сообщений в чат.
 * Управляет состоянием текстового поля, его автоматическим расширением
 * и отображением кнопок "Отправить" или "Стоп" с плавной анимацией.
 * @param {ChatInputProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const ChatInput: FC<ChatInputProps> = ({
  onSendMessage,
  onStopGenerating,
  isLoading,
  showScrollButton,
  onScrollToBottom
}) => {
  const [inputText, setInputText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  /**
   * Эффект для автоматического изменения высоты поля ввода при изменении текста.
   */
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto'; // Сбрасываем высоту для корректного пересчета
      textarea.style.height = `${textarea.scrollHeight}px`;
    }
  }, [inputText]);

  /**
   * Обработчик отправки формы.
   * @param {FormEvent} e - Событие формы.
   */
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (inputText.trim() && !isLoading) {
      onSendMessage(inputText);
      setInputText('');
    }
  };

  /**
   * Обработчик нажатия клавиш для отправки по Enter.
   * @param {KeyboardEvent<HTMLTextAreaElement>} e - Событие клавиатуры.
   */
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e as unknown as FormEvent);
    }
  };

  return (
    <div className={styles.chatInputContainer}>
      {showScrollButton && <ScrollToBottomButton onClick={() => onScrollToBottom()} />}
      <form onSubmit={handleSubmit} className={styles.chatInputForm}>
        <textarea
          ref={textareaRef}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Спросите что-нибудь..."
          className={styles.textarea}
          rows={1}
          aria-label="Поле для ввода сообщения"
        />
        <div className={styles.buttonContainer}>
          <button
            type="submit"
            disabled={!inputText.trim() || isLoading}
            className={`${styles.sendButton} ${!isLoading ? styles.visible : styles.hidden}`}
            aria-label="Отправить сообщение"
          >
            <Send size={20} />
          </button>
          <button
            type="button"
            onClick={onStopGenerating}
            className={`${styles.stopButton} ${isLoading ? styles.visible : styles.hidden}`}
            aria-label="Остановить генерацию"
          >
            <Square size={20} />
          </button>
        </div>
      </form>
    </div>
  );
};
