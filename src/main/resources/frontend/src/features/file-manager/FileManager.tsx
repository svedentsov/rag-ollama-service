import React from 'react';
import { useFiles } from './useFiles';
import { FileUpload } from './FileUpload';
import { FileList } from './FileList';
import styles from './FileManager.module.css'; // Этот путь теперь верный

/**
 * Компонент-контейнер для раздела "Файловый менеджер".
 * Он является "умным" компонентом, который управляет состоянием через хук `useFiles`
 * и оркеструет дочерние "глупые" компоненты.
 *
 * @returns {React.ReactElement}
 */
export const FileManager: React.FC = () => {
  const { files, isLoading, error, uploadFile, deleteFile, isUploading } = useFiles();

  const handleUpload = (file: File) => {
    uploadFile(file);
  };

  const handleDelete = (fileId: string) => {
    if (window.confirm("Вы уверены, что хотите удалить этот файл? Это действие необратимо.")) {
      deleteFile(fileId);
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return <div className={styles.centered}><div className={styles.spinner} role="status" aria-label="Загрузка файлов"></div></div>;
    }
    if (error) {
      return <div className={styles.centered}><div className={styles.errorAlert}>Ошибка загрузки файлов: {error.message}</div></div>;
    }
    return <FileList files={files} onDelete={handleDelete} />;
  };

  return (
    <div className={styles.fileManagerContainer}>
      <header className={styles.header}>
        <h1>Файловый менеджер</h1>
        <FileUpload onUpload={handleUpload} isUploading={isUploading} />
      </header>
      <div className={styles.fileListContainer}>
        {renderContent()}
      </div>
    </div>
  );
};
