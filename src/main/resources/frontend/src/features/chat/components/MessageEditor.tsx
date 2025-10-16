import React, { useState, useRef, useEffect, FC } from 'react';
import { useClickOutside } from '../../../hooks/useClickOutside';
import styles from './ChatMessage.module.css';

/**
 * @interface MessageEditorProps
 * @description Пропсы для компонента MessageEditor.
 */
export interface MessageEditorProps {
  /** @param {string} initialText - Начальный текст для редактирования. */
  initialText: string;
  /** @param {(newContent: string) => void} onSave - Колбэк при сохранении изменений. */
  onSave: (newContent: string) => void;
  /** @param {() => void} onCancel - Колбэк при отмене редактирования. */
  onCancel: () => void;
  /** @param {string} [saveButtonText="Сохранить"] - Текст для кнопки сохранения. */
  saveButtonText?: string;
}

/**
 * Компонент для встроенного редактирования текста сообщения.
 * @param {MessageEditorProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент редактора.
 */
export const MessageEditor: FC<MessageEditorProps> = ({
  initialText,
  onSave,
  onCancel,
  saveButtonText = "Сохранить"
}) => {
  const [editText, setEditText] = useState(initialText);
  const editorRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useClickOutside(editorRef, onCancel);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.focus();
      textarea.style.height = 'auto';
      textarea.style.height = `${textarea.scrollHeight}px`;
      textarea.setSelectionRange(textarea.value.length, textarea.value.length);
    }
  }, []);

  const handleSave = () => {
    if (editText.trim() && editText.trim() !== initialText.trim()) {
      onSave(editText.trim());
    } else {
      onCancel();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSave();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onCancel();
    }
  };

  const handleTextareaInput = (e: React.FormEvent<HTMLTextAreaElement>) => {
    const textarea = e.currentTarget;
    textarea.style.height = 'auto';
    textarea.style.height = `${textarea.scrollHeight}px`;
  };

  return (
    <div ref={editorRef} className={styles.editContainer}>
        <div className={styles.bubbleContent}>
            <textarea
                ref={textareaRef}
                value={editText}
                onChange={(e) => setEditText(e.target.value)}
                onKeyDown={handleKeyDown}
                onInput={handleTextareaInput}
                className={styles.editTextarea}
                aria-label="Редактирование сообщения"
            />
        </div>
        <div className={styles.editActions}>
            <button onClick={handleSave} className={styles.saveButton}>{saveButtonText}</button>
            <button onClick={onCancel} className={styles.cancelButton}>Отмена</button>
        </div>
    </div>
  );
};
