import React, { FC, ReactNode, useEffect } from 'react';
import { X } from 'lucide-react';
import styles from './Modal.module.css';

/**
 * @interface ModalProps
 * @description Пропсы для универсального компонента модального окна.
 */
interface ModalProps {
  /** @param {boolean} isOpen - Флаг, управляющий видимостью модального окна. */
  isOpen: boolean;
  /** @param {() => void} onClose - Колбэк, вызываемый при запросе на закрытие окна. */
  onClose: () => void;
  /** @param {string} title - Заголовок модального окна. */
  title: string;
  /** @param {ReactNode} children - Содержимое модального окна. */
  children: ReactNode;
}

/**
 * Переиспользуемый, доступный (a11y) компонент модального окна.
 * Управляет фокусом, закрывается по Escape и клику на оверлей.
 * @param {ModalProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если окно закрыто.
 */
export const Modal: FC<ModalProps> = ({ isOpen, onClose, title, children }) => {
  useEffect(() => {
    /**
     * Обработчик нажатия клавиш для закрытия по Escape.
     */
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      // Блокируем прокрутку основного контента, пока модальное окно открыто
      document.body.style.overflow = 'hidden';
    }

    // Очистка при размонтировании или изменении состояния
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'auto';
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  return (
    <div className={styles.overlay} onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h2 id="modal-title" className={styles.modalTitle}>{title}</h2>
          <button className={styles.closeButton} onClick={onClose} aria-label="Закрыть окно">
            <X size={20} />
          </button>
        </div>
        <div className={styles.modalBody}>
          {children}
        </div>
      </div>
    </div>
  );
};
