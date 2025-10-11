import React, { useState, useRef, useEffect, FC } from 'react';
import { Send, Square } from 'lucide-react';
import { ScrollToBottomButton } from "./ScrollToBottomButton";
import styles from './ChatInput.module.css';

/**
 * @interface ChatInputProps
 * @description Пропсы для компонента ChatInput.
 */
interface ChatInputProps {
  /** @param {(text: string) => void} onSendMessage - Колбэк для отправки нового сообщения. */
  onSendMessage: (text: string) => void;
  /** @param {() => void} onStopGenerating - Колбэк для остановки всех активных генераций. */
  onStopGenerating: () => void;
  /** @param {boolean} isLoading - Флаг, указывающий, активен ли хотя бы один процесс генерации. */
  isLoading: boolean;
  /** @param {boolean} showScrollButton - Флаг для отображения кнопки прокрутки вниз. */
  showScrollButton: boolean;
  /** @param {() => void} onScrollToBottom - Колбэк для плавной прокрутки вниз. */
  onScrollToBottom: () => void;
}

/**
 * Компонент для ввода и отправки сообщений в чат.
 * Управляет состоянием текстового поля, его автоматическим расширением
 * и отображением кнопок "Отправить" или "Стоп" с плавной анимацией.
 * @param {ChatInputProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const ChatInput: FC<ChatInputProps> = ({ onSendMessage, onStopGenerating, isLoading, showScrollButton, onScrollToBottom }) => {
    const [inputText, setInputText] = useState('');
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = 'auto'; // Сбрасываем высоту
            const scrollHeight = textarea.scrollHeight;
            textarea.style.height = `${scrollHeight}px`;
        }
    }, [inputText]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (inputText.trim()) {
            onSendMessage(inputText);
            setInputText('');
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit(e as unknown as React.FormEvent);
        }
    };

    return (
        <div className={styles.chatInputContainer}>
            {showScrollButton && <ScrollToBottomButton onClick={onScrollToBottom} />}
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
                {/*
                  Контейнер теперь управляет состоянием анимации.
                  Кнопки просто меняют свою видимость.
                */}
                <div className={`${styles.buttonContainer} ${isLoading ? styles.loading : ''}`}>
                    <button
                        type="submit"
                        disabled={!inputText.trim()}
                        className={`${styles.actionButton} ${styles.sendButton} ${!isLoading ? styles.visible : styles.hidden}`}
                        aria-label="Отправить сообщение"
                    >
                        <Send size={18} />
                    </button>
                    <button
                        type="button"
                        onClick={onStopGenerating}
                        className={`${styles.actionButton} ${styles.stopButton} ${isLoading ? styles.visible : styles.hidden}`}
                        aria-label="Остановить генерацию"
                    >
                        <Square size={18} />
                    </button>
                </div>
            </form>
        </div>
    );
}
