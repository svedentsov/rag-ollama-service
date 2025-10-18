import React from 'react';
import { FileDto } from '../../../types';
import { FileCard } from './FileCard';
import { ArrowUpDown } from 'lucide-react';
import styles from './FileList.module.css';

/**
 * @interface FileListProps
 * @description Пропсы для компонента FileList.
 */
interface FileListProps {
  files: FileDto[];
  sort: { id: string; desc: boolean };
  onSort: (columnId: string) => void;
  onDelete: (fileId: string) => void;
  onView: (file: FileDto) => void;
}

/**
 * Презентационный компонент для отображения списка файлов.
 * Теперь использует CSS Grid вместо таблицы для большей гибкости и семантической корректности.
 * @param {FileListProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileList: React.FC<FileListProps> = ({ files, sort, onSort, onDelete, onView }) => {

  const SortableHeader: React.FC<{ columnId: string; children: React.ReactNode }> = ({ columnId, children }) => (
    <button onClick={() => onSort(columnId)} className={styles.sortButton}>
      {children}
      <ArrowUpDown size={14} />
    </button>
  );

  if (files.length === 0) {
    return <p className={styles.emptyState}>Файлы еще не загружены.</p>;
  }

  return (
    <div className={styles.fileGrid}>
      <div className={styles.gridHeader}><SortableHeader columnId="fileName">Имя файла</SortableHeader></div>
      <div className={styles.gridHeader}><SortableHeader columnId="fileSize">Размер</SortableHeader></div>
      <div className={styles.gridHeader}><SortableHeader columnId="createdAt">Дата загрузки</SortableHeader></div>
      <div className={`${styles.gridHeader} ${styles.actionsColumn}`}>Действия</div>
      
      {files.map(file => (
        <FileCard
          key={file.id}
          file={file}
          onDelete={onDelete}
          onView={onView}
        />
      ))}
    </div>
  );
};
