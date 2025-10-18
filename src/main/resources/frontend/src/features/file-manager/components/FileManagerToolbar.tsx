import React, { FC } from 'react';
import { SearchInput } from '../../../components/SearchInput';
import styles from './FileManagerToolbar.module.css';
import { FileUpload } from './FileUpload';

/**
 * @interface FileManagerToolbarProps
 * @description Пропсы для панели инструментов файлового менеджера.
 */
interface FileManagerToolbarProps {
  /** @param onUpload - Колбэк, вызываемый при выборе файлов для загрузки. */
  onUpload: (files: File[]) => void;
  /** @param isUploading - Флаг, указывающий на процесс загрузки. */
  isUploading: boolean;
  /** @param searchTerm - Текущее значение поискового запроса. */
  searchTerm: string;
  /** @param onSearchChange - Колбэк при изменении поискового запроса. */
  onSearchChange: (value: string) => void;
}

/**
 * Компонент панели инструментов для файлового менеджера.
 * @param {FileManagerToolbarProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileManagerToolbar: FC<FileManagerToolbarProps> = ({
  onUpload, isUploading, searchTerm, onSearchChange
}) => {
  return (
    <header className={styles.header}>
      <div className={styles.leftActions}>
        <h1>Файлы</h1>
      </div>
      <div className={styles.rightActions}>
        <SearchInput
          value={searchTerm}
          onChange={onSearchChange}
          placeholder="Поиск по файлам..."
        />
        <FileUpload onUpload={onUpload} isUploading={isUploading} />
      </div>
    </header>
  );
};
