import React from 'react';
import { FileDto } from '../../types';
import { FileListItem } from './FileListItem';
import styles from './FileList.module.css'; // Этот путь теперь верный

interface FileListProps {
  files: FileDto[];
  onDelete: (fileId: string) => void;
}

/**
 * Презентационный компонент для отображения списка файлов.
 * @param {FileListProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileList: React.FC<FileListProps> = ({ files, onDelete }) => {
  if (files.length === 0) {
    return <p className={styles.emptyState}>Файлы еще не загружены.</p>;
  }

  return (
    <table className={styles.fileTable}>
      <thead>
        <tr>
          <th>Имя файла</th>
          <th>Размер</th>
          <th>Дата загрузки</th>
          <th>Действия</th>
        </tr>
      </thead>
      <tbody>
        {files.map(file => (
          <FileListItem key={file.id} file={file} onDelete={onDelete} />
        ))}
      </tbody>
    </table>
  );
};
