import React, { FC, useRef, ChangeEvent, useState, DragEvent } from 'react';
import { Upload } from 'lucide-react';
import styles from './FileManagerToolbar.module.css';

/**
 * @interface FileUploadProps
 * @description Пропсы для компонента FileUpload.
 */
interface FileUploadProps {
  /** @param {(files: File[]) => void} onUpload - Колбэк, вызываемый при выборе файлов. Принимает массив файлов. */
  onUpload: (files: File[]) => void;
  /** @param {boolean} isUploading - Флаг, указывающий на процесс загрузки. */
  isUploading: boolean;
}

/**
 * Презентационный компонент для загрузки файлов с поддержкой Drag-and-Drop.
 * @param {FileUploadProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
export const FileUpload: FC<FileUploadProps> = ({ onUpload, isUploading }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFiles = (files: FileList | null) => {
    if (files && files.length > 0) {
      onUpload(Array.from(files));
    }
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    handleFiles(event.target.files);
  };

  const handleDragEnter = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    handleFiles(e.dataTransfer.files);
  };

  return (
    <>
      <input
        type="file"
        multiple
        ref={fileInputRef}
        onChange={handleFileChange}
        style={{ display: 'none' }}
        accept=".txt,.md,.json,.csv,.pdf,.docx,.java,.log,.xml,.yaml,.yml"
        disabled={isUploading}
      />
      <div
        onDragEnter={handleDragEnter}
        onDragOver={handleDragEnter}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`${styles.dropzone} ${isDragging ? styles.dragging : ''}`}
      >
        <button
          className={styles.uploadButton}
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}
        >
          <Upload size={16} />
          {isUploading ? 'Загрузка...' : 'Загрузить файлы'}
        </button>
      </div>
    </>
  );
};
