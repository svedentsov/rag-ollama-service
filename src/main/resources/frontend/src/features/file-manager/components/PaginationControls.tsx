import React, { FC } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import styles from './PaginationControls.module.css';

interface PaginationControlsProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const PaginationControls: FC<PaginationControlsProps> = ({ page, totalPages, onPageChange }) => {
  return (
    <div className={styles.paginationContainer}>
      <button onClick={() => onPageChange(page - 1)} disabled={page === 0} className={styles.pageButton}>
        <ChevronLeft size={16} />
        <span>Назад</span>
      </button>
      <span className={styles.pageInfo}>
        Страница {page + 1} из {totalPages}
      </span>
      <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1} className={styles.pageButton}>
        <span>Вперед</span>
        <ChevronRight size={16} />
      </button>
    </div>
  );
};
