import React from 'react';
import { Trash2 } from 'lucide-react';
import { FileDto } from '../../types';
// ИСПРАВЛЕНИЕ: Исправлено имя файла
import { useFileSelectionStore } from '../../state/useFileSelectionStore';
import { useSessionStore } from '../../state/sessionStore';
import styles from './FileList.module.css';

interface FileListItemProps {
  file: FileDto;
  onDelete: (fileId: string) => void;
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const FileListItem: React.FC<FileListItemProps> = ({ file, onDelete }) => {
  const currentSessionId = useSessionStore(state => state.currentSessionId);
  const { toggleSelection, getSelectedIds } = useFileSelectionStore();
  const isSelected = currentSessionId ? getSelectedIds(currentSessionId).has(file.id) : false;

  const handleToggle = () => {
    if (currentSessionId) {
      toggleSelection(currentSessionId, file.id);
    }
  };

  return (
    <tr className={styles.fileRow}>
      <td>
        <input
          type="checkbox"
          checked={isSelected}
          onChange={handleToggle}
          disabled={!currentSessionId}
          title={!currentSessionId ? "Сначала выберите или создайте чат" : "Выбрать файл для контекста"}
          className={styles.checkbox}
          aria-label={`Выбрать файл ${file.name}`}
        />
      </td>
      <td data-label="Имя файла">{file.name}</td>
      <td data-label="Размер">{formatFileSize(file.size)}</td>
      <td data-label="Дата загрузки">{new Date(file.uploadedAt).toLocaleString()}</td>
      <td data-label="Действия">
        <button
          className={styles.deleteButton}
          onClick={() => onDelete(file.id)}
          aria-label={`Удалить файл ${file.name}`}
        >
          <Trash2 size={16} />
        </button>
      </td>
    </tr>
  );
};
