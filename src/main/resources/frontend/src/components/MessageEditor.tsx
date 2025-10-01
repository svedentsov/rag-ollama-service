import React, { useState, useRef, useEffect, FC } from 'react';
import styles from './ChatMessage.module.css';

/**
 * Пропсы для компонента MessageEditor.
 * @internal
 */
export interface MessageEditorProps {
  /** @param initialText - Начальный текст для редактирования. */
  initialText: string;
  /** @param onSave - Колбэк при сохранении изменений. */
  onSave: (newContent: string) => void;
  /** @param onCancel - Колбэк при отмене редактирования. */
  onCancel: () => void;
}

/**
 * Суб-компонент, инкапсулирующий форму и логику редактирования сообщения.
 * @param {MessageEditorProps} props - Пропсы компонента.
 * @internal
 */
export const MessageEditor: FC<MessageEditorProps> = ({ initialText, onSave, onCancel }) => {
  const [editText, setEditText] = useState(initialText);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

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
    if (editText.trim() && editText !== initialText) {
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

  return (
    <div className={styles.editContainer}>
      <textarea
        ref={textareaRef}
        value={editText}
        onChange={(e) => setEditText(e.target.value)}
        onKeyDown={handleKeyDown}
        className={styles.editTextarea}
        aria-label="Редактирование сообщения"
      />
      <div className={styles.editActions}>
        <button onClick={handleSave} className={styles.saveButton}>Сохранить и отправить</button>
        <button onClick={onCancel} className={styles.cancelButton}>Отмена</button>
      </div>
    </div>
  );
};
