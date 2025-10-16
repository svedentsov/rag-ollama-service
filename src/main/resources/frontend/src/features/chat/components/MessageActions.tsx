import React, { FC } from 'react';
import { RefreshCw, Edit, Trash, ThumbsUp, ThumbsDown, Square, Copy } from 'lucide-react';
import toast from 'react-hot-toast';
import { Message } from '../types';
import styles from './ChatMessage.module.css';

/**
 * @interface MessageActionsProps
 * @description Пропсы для компонента с кнопками действий над сообщением.
 */
interface MessageActionsProps {
  /** @param {Message} message - Объект сообщения, для которого отображаются действия. */
  message: Message;
  /** @param {boolean} isLastInTurn - Является ли это сообщение последним в текущем "ходе" диалога. */
  isLastInTurn: boolean;
  /** @param {boolean} isFeedbackSending - Флаг, указывающий на процесс отправки обратной связи. */
  isFeedbackSending: boolean;
  /** @param {() => void} onRegenerate - Колбэк для запроса повторной генерации ответа. */
  onRegenerate: () => void;
  /** @param {() => void} onStartEdit - Колбэк для переключения сообщения в режим редактирования. */
  onStartEdit: () => void;
  /** @param {() => void} onDelete - Колбэк для удаления сообщения. */
  onDelete: () => void;
  /** @param {() => void} onStop - Колбэк для остановки потоковой генерации. */
  onStop: () => void;
  /** @param {(isHelpful: boolean) => void} onSendFeedback - Колбэк для отправки обратной связи (оценки). */
  onSendFeedback: (isHelpful: boolean) => void;
}

/**
 * Презентационный компонент, который отображает набор кнопок действий
 * для конкретного сообщения (регенерация, редактирование, оценка и т.д.).
 * Является "глупым" компонентом, вся логика передается через пропсы.
 *
 * @param {MessageActionsProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const MessageActions: FC<MessageActionsProps> = ({
  message, isLastInTurn, isFeedbackSending, onRegenerate, onStartEdit, onDelete, onStop, onSendFeedback,
}) => {
  const isUser = message.type === 'user';

  /**
   * Обработчик копирования текста сообщения в буфер обмена.
   */
  const handleCopy = () => {
    navigator.clipboard.writeText(message.text)
      .then(() => toast.success('Скопировано в буфер обмена!'))
      .catch(() => toast.error('Не удалось скопировать.'));
  };

  if (message.isStreaming) {
    return (
      <button className={styles.actionButton} onClick={onStop} data-tooltip="Остановить генерацию" aria-label="Остановить генерацию">
        <Square size={18} />
      </button>
    );
  }

  return (
    <>
      {!isUser && message.taskId && (
        <>
          <button className={styles.actionButton} onClick={() => onSendFeedback(true)} disabled={isFeedbackSending} data-tooltip="Полезный ответ" aria-label="Полезный ответ">
            <ThumbsUp size={18} />
          </button>
          <button className={styles.actionButton} onClick={() => onSendFeedback(false)} disabled={isFeedbackSending} data-tooltip="Бесполезный ответ" aria-label="Бесполезный ответ">
            <ThumbsDown size={18} />
          </button>
        </>
      )}
      {!isUser && isLastInTurn && (
        <button className={styles.actionButton} onClick={onRegenerate} data-tooltip="Повторить генерацию" aria-label="Повторить генерацию">
          <RefreshCw size={18} />
        </button>
      )}
      <button className={styles.actionButton} onClick={handleCopy} data-tooltip="Скопировать" aria-label="Скопировать">
        <Copy size={18} />
      </button>
      <button className={styles.actionButton} onClick={onStartEdit} data-tooltip="Редактировать" aria-label="Редактировать">
        <Edit size={18} />
      </button>
      <button className={styles.actionButton} onClick={onDelete} data-tooltip="Удалить" aria-label="Удалить">
        <Trash size={18} />
      </button>
    </>
  );
};
