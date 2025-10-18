import React, { FC, useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useFileList } from './useFileList';
import { useFileUpload } from './useFileUpload';
import { FileList } from './components/FileList';
import { FileManagerToolbar } from './components/FileManagerToolbar';
import { FileUploadProgress } from './components/FileUploadProgress';
import { PaginationControls } from './components/PaginationControls';
import { Modal } from '../../components/Modal';
import { CodeBlock } from '../chat/components/CodeBlock';
import { useDebounce } from '../../hooks/useDebounce';
import { api } from '../../api';
import { FileDto } from '../../types';
import styles from './FileManager.module.css';

/**
 * Компонент-страница для "Файлового менеджера", выступающий в роли оркестратора.
 * Он использует специализированные хуки для получения данных и управления состоянием,
 * а затем передает их в "глупые" презентационные компоненты.
 * @returns {React.ReactElement} Отрендеренный компонент страницы.
 */
export const FileManagerPage: FC = () => {
  const [pagination, setPagination] = useState({ page: 0, size: 20 });
  const [sorting, setSorting] = useState({ id: 'fileName', desc: false }); // Изменена сортировка по умолчанию
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 300);
  const [viewingFile, setViewingFile] = useState<FileDto | null>(null);

  const {
    pageData, isLoading, error, deleteFiles
  } = useFileList(pagination.page, pagination.size, sorting, debouncedSearchTerm);

  const { uploads, uploadFilesAsync, isUploading } = useFileUpload();

  const { data: fileContent, isLoading: isContentLoading } = useQuery({
    queryKey: ['fileContent', viewingFile?.id],
    queryFn: () => viewingFile ? api.getFileContent(viewingFile.id) : Promise.resolve(''),
    enabled: !!viewingFile,
  });

  const handleDelete = useCallback((fileIds: string[]) => {
    if (window.confirm(`Вы уверены, что хотите удалить ${fileIds.length} файл(а)?`)) {
      deleteFiles(fileIds);
    }
  }, [deleteFiles]);

  const handleSort = useCallback((columnId: string) => {
    setSorting(current => ({
      id: columnId,
      desc: current.id === columnId ? !current.desc : false, // Инвертируем направление при повторном клике
    }));
  }, []);

  const renderContent = () => {
    if (isLoading) return <div className={styles.centered}><div className={styles.spinner} /></div>;
    if (error) return <div className={styles.centered}><div className={styles.errorAlert}>{error.message}</div></div>;
    return (
        <>
            <FileList
                files={pageData?.content ?? []}
                sort={sorting}
                onSort={handleSort}
                onDelete={(fileId) => handleDelete([fileId])}
                onView={setViewingFile}
            />
            {pageData && pageData.totalPages > 1 && (
                <PaginationControls
                    page={pagination.page}
                    totalPages={pageData.totalPages}
                    onPageChange={(p) => setPagination(prev => ({...prev, page: p}))}
                />
            )}
        </>
    );
  };

  return (
    <div className={styles.fileManagerContainer}>
      <FileManagerToolbar
        onUpload={(files: File[]) => uploadFilesAsync(files).then(() => {
            setPagination(prev => ({ ...prev, page: 0 }));
        })}
        isUploading={isUploading}
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
      />
      <div className={styles.fileListContainer}>
        {uploads.length > 0 && <FileUploadProgress uploads={uploads} />}
        {renderContent()}
      </div>
      <Modal isOpen={!!viewingFile} onClose={() => setViewingFile(null)} title={viewingFile?.fileName || ''}>
        {isContentLoading
          ? <div className={styles.centered}><div className={styles.spinner}/></div>
          : <CodeBlock language="text">{fileContent || ""}</CodeBlock>
        }
      </Modal>
    </div>
  );
};
