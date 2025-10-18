import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { v4 as uuidv4 } from 'uuid';
import { api } from '../../api';
import { FileDto, UploadProgress } from '../../types';

const CONCURRENT_UPLOADS = 3;
const FILES_QUERY_KEY = 'files';

type SingleUploadVariables = { file: File; uploadId: string };

/**
 * @typedef UploadResult
 * @description Результат выполнения пакетной загрузки.
 * @property {FileDto[]} successful - Список успешно загруженных файлов.
 * @property {File[]} failed - Список файлов, которые не удалось загрузить.
 */
type UploadResult = {
    successful: FileDto[];
    failed: File[];
};

/**
 * Хук для инкапсуляции полной логики загрузки файлов.
 * Управляет очередью загрузок, состоянием прогресса и взаимодействием с API.
 * @returns Объект, содержащий состояние загрузок и функции для их управления.
 */
export function useFileUpload() {
    const queryClient = useQueryClient();
    const [uploads, setUploads] = useState<UploadProgress[]>([]);

    const singleUploadMutation = useMutation({
        mutationFn: (vars: SingleUploadVariables) => api.uploadFile(vars.file),
        onSuccess: (newFile, variables) => {
            setUploads(prev => prev.map(u => u.id === variables.uploadId ? { ...u, status: 'success', progress: 100 } : u));
        },
        onError: (err: Error, variables) => {
            setUploads(prev => prev.map(u => u.id === variables.uploadId ? { ...u, status: 'error', error: err.message } : u));
            toast.error(`Ошибка при загрузке файла: ${variables.file.name}`);
        },
    });

    /**
     * Асинхронно загружает массив файлов с ограниченным параллелизмом.
     * @param filesToUpload - Массив файлов для загрузки.
     * @returns {Promise<UploadResult>} Промис, который разрешается с результатом загрузки.
     */
    const uploadFilesAsync = async (filesToUpload: File[]): Promise<UploadResult> => {
        const newUploads: UploadProgress[] = filesToUpload.map(file => ({
            id: uuidv4(), file, status: 'pending', progress: 0,
        }));
        setUploads(prev => [...prev, ...newUploads]);

        const successful: FileDto[] = [];
        const failed: File[] = [];

        const uploadQueue = [...newUploads];
        const worker = async () => {
            while (uploadQueue.length > 0) {
                const task = uploadQueue.shift();
                if (task) {
                    setUploads(prev => prev.map(u => u.id === task.id ? { ...u, status: 'uploading' } : u));
                    try {
                        const newFile = await singleUploadMutation.mutateAsync({ file: task.file, uploadId: task.id });
                        successful.push(newFile);
                    } catch (e) {
                        failed.push(task.file);
                    }
                }
            }
        };

        const workers = Array.from({ length: CONCURRENT_UPLOADS }, () => worker());
        await Promise.all(workers);

        if (successful.length > 0) {
            toast.success(`${successful.length} из ${filesToUpload.length} файлов успешно загружено!`);
            await queryClient.invalidateQueries({ queryKey: [FILES_QUERY_KEY] });
        }

        setTimeout(() => setUploads(prev => prev.filter(u => u.status === 'error')), 5000);

        return { successful, failed };
    };

    return {
        uploads,
        uploadFilesAsync,
        isUploading: singleUploadMutation.isPending || uploads.some(u => ['pending', 'uploading'].includes(u.status)),
    };
}
