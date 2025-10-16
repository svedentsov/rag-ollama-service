import React, { useRef } from 'react';
import styles from './FileManager.module.css';

interface FileUploadProps {
  onUpload: (file: File) => void;
  isUploading: boolean;
}

/**
 * Презентационный компонент для загрузки файла.
 * @param {FileUploadProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileUpload: React.FC<FileUploadProps> = ({ onUpload, isUploading }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      onUpload(file);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <>
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        style={{ display: 'none' }}
        accept=".txt,.md,.json,.csv,.pdf,.docx"
        disabled={isUploading}
      />
      <button
        className={styles.uploadButton}
        onClick={() => fileInputRef.current?.click()}
        disabled={isUploading}
      >
        {isUploading ? 'Загрузка...' : 'Загрузить файл'}
      </button>
    </>
  );
};
