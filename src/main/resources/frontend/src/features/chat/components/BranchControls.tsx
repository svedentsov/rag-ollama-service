import React, { FC } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import styles from './ChatMessage.module.css';

/**
 * @interface BranchControlsProps
 * @description Пропсы для компонента управления ветвлением.
 */
interface BranchControlsProps {
  /** @param {number} current - Номер текущей активной ветки. */
  current: number;
  /** @param {number} total - Общее количество веток. */
  total: number;
  /** @param {() => void} onPrev - Колбэк для перехода к предыдущей ветке. */
  onPrev: () => void;
  /** @param {() => void} onNext - Колбэк для перехода к следующей ветке. */
  onNext: () => void;
}

/**
 * Презентационный компонент для отображения UI управления ветвлением ответов.
 * @param {BranchControlsProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const BranchControls: FC<BranchControlsProps> = ({ current, total, onPrev, onNext }) => {
  return (
    <div className={styles.branchControls}>
      <button
        onClick={onPrev}
        disabled={current <= 1}
        className={styles.branchButton}
        data-tooltip="Предыдущий ответ"
        aria-label="Предыдущий ответ"
      >
        <ChevronLeft size={18} />
      </button>
      <span className={styles.branchIndicator}>{current} / {total}</span>
      <button
        onClick={onNext}
        disabled={current >= total}
        className={styles.branchButton}
        data-tooltip="Следующий ответ"
        aria-label="Следующий ответ"
      >
        <ChevronRight size={18} />
      </button>
    </div>
  );
};
