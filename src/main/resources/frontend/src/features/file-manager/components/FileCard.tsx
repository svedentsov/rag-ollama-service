import React, { FC } from 'react';
import { Trash2, Eye } from 'lucide-react';
import { FileDto } from '../../../types';
import { formatFileSize, formatDate } from '../../../utils/formatters';
import { FileTypeIcon } from '../../../components/FileTypeIcon';
import styles from './FileCard.module.css';

/**
 * @interface FileCardProps
 * @description Пропсы для компонента FileCard, представляющего один файл.
 */
interface FileCardProps {
  /** @param {FileDto} file - Объект с метаданными файла. */
  file: FileDto;
  /** @param {(fileId: string) => void} onDelete - Колбэк для удаления файла. */
  onDelete: (fileId: string) => void;
  /** @param {(file: FileDto) => void} onView - Колбэк для открытия просмотра файла. */
  onView: (file: FileDto) => void;
}

/**
 * Компонент для отображения одного файла. Теперь это семантически верная кнопка,
 * что улучшает доступность и упрощает обработку кликов.
 * @param {FileCardProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileCard: FC<FileCardProps> = ({ file, onDelete, onView }) => {
  return (
    <button className={styles.fileRowButton} onClick={() => onView(file)} aria-label={`Просмотреть файл ${file.fileName}`}>
      <div className={styles.fileNameCell}>
        <FileTypeIcon mimeType={file.mimeType} />
        <span>{file.fileName}</span>
      </div>
      <div className={styles.fileDetailCell}>{formatFileSize(file.fileSize)}</div>
      <div className={styles.fileDetailCell}>{formatDate(file.createdAt)}</div>
      <div className={styles.actionsCell} onClick={e => e.stopPropagation()}>
        <button
          className={`${styles.actionButton} ${styles.deleteButton}`}
          onClick={() => onDelete(file.id)}
          aria-label={`Удалить файл ${file.fileName}`}
        >
          <Trash2 size={16} />
        </button>
      </div>
    </button>
  );
};
