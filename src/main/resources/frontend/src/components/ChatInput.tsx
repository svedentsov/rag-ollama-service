import React, { useState, useRef, useEffect } from 'react';
import { Send, Square } from 'lucide-react';
import styles from './ChatInput.module.css';

interface ChatInputProps {
    onSendMessage: (text: string) => void;
    onStopGenerating: () => void;
    isLoading: boolean;
}

export function ChatInput({ onSendMessage, onStopGenerating, isLoading }: ChatInputProps) {
    const [inputText, setInputText] = useState('');
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = '';
            const scrollHeight = textarea.scrollHeight;
            textarea.style.height = `${scrollHeight}px`;
        }
    }, [inputText]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (inputText.trim() && !isLoading) {
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
            <form onSubmit={handleSubmit} className={styles.chatInputForm}>
                <textarea
                    ref={textareaRef}
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Спросите что-нибудь..."
                    className={styles.textarea}
                    rows={1}
                />
                {isLoading ? (
                    <button type="button" onClick={onStopGenerating} className={styles.stopButton} aria-label="Остановить генерацию">
                        <Square size={18} />
                    </button>
                ) : (
                    <button type="submit" disabled={!inputText.trim()} className={styles.sendButton} aria-label="Отправить сообщение">
                        <Send size={18} />
                    </button>
                )}
            </form>
        </div>
    );
}
