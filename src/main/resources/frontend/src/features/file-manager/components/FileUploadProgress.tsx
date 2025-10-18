import React, { FC } from 'react';
import { CheckCircle, AlertCircle, X, Loader } from 'lucide-react';
import { UploadProgress } from '../../../types';
import { FileTypeIcon } from '../../../components/FileTypeIcon';
import { formatFileSize } from '../../../utils/formatters';
import styles from './FileUploadProgress.module.css';

/**
 * @interface FileUploadProgressProps
 * @description Пропсы для компонента FileUploadProgress.
 */
interface FileUploadProgressProps {
  /** @param {UploadProgress[]} uploads - Список активных загрузок. */
  uploads: UploadProgress[];
}

/**
 * Компонент для отображения прогресса загрузки нескольких файлов.
 * @param {FileUploadProgressProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileUploadProgress: FC<FileUploadProgressProps> = ({ uploads }) => {
  return (
    <div className={styles.progressContainer}>
      <h3 className={styles.progressTitle}>Загрузки</h3>
      {uploads.map(upload => (
        <div key={upload.id} className={styles.progressItem}>
          <FileTypeIcon mimeType={upload.file.type} />
          <div className={styles.fileInfo}>
            <span className={styles.fileName}>{upload.file.name}</span>
            <span className={styles.fileSize}>{formatFileSize(upload.file.size)}</span>
          </div>
          <div className={styles.statusWrapper}>
            {upload.status === 'uploading' && (
              <div className={styles.progressBar}>
                {/* Для демонстрации прогресс всегда 50%. В реальном приложении он будет динамическим. */}
                <div className={styles.progressFill} style={{ width: `50%` }} />
              </div>
            )}
            {upload.status === 'success' && <CheckCircle size={20} className={styles.successIcon} />}
            {upload.status === 'error' && <AlertCircle size={20} className={styles.errorIcon} title={upload.error} />}
            {upload.status === 'pending' && <Loader size={16} className={styles.pendingIcon} />}
          </div>
        </div>
      ))}
    </div>
  );
};
