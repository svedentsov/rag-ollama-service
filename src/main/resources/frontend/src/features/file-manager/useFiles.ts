import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { api } from '../../api';
import { FileDto } from '../../types';

const FILES_QUERY_KEY = ['files'];

/**
 * @description Хук для управления состоянием файлов. Инкапсулирует всю логику
 * получения, загрузки и удаления файлов, используя React Query для кэширования,
 * фонового обновления и оптимистичных мутаций.
 *
 * @returns {{
 *   files: FileDto[],
 *   isLoading: boolean,
 *   error: Error | null,
 *   uploadFile: (file: File) => void,
 *   deleteFile: (fileId: string) => void,
 *   isUploading: boolean,
 *   isDeleting: boolean
 * }} Объект с данными о файлах и функциями-мутациями.
 */
export function useFiles() {
    const queryClient = useQueryClient();

    const { data: files = [], isLoading, error } = useQuery<FileDto[]>({
        queryKey: FILES_QUERY_KEY,
        queryFn: api.getFiles,
    });

    const uploadMutation = useMutation({
        mutationFn: api.uploadFile,
        onSuccess: () => {
            toast.success('Файл успешно загружен!');
            return queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
        },
        onError: (err: Error) => {
            toast.error(`Ошибка загрузки: ${err.message}`);
        }
    });

    const deleteMutation = useMutation({
        mutationFn: api.deleteFile,
        onMutate: async (fileIdToDelete: string) => {
            await queryClient.cancelQueries({ queryKey: FILES_QUERY_KEY });
            const previousFiles = queryClient.getQueryData<FileDto[]>(FILES_QUERY_KEY) ?? [];
            queryClient.setQueryData<FileDto[]>(FILES_QUERY_KEY, old =>
                old?.filter(file => file.id !== fileIdToDelete)
            );
            return { previousFiles };
        },
        onError: (err, variables, context) => {
            if (context?.previousFiles) {
                queryClient.setQueryData(FILES_QUERY_KEY, context.previousFiles);
            }
            toast.error('Не удалось удалить файл.');
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
        },
    });

    return {
        files,
        isLoading,
        error: error as Error | null,
        uploadFile: uploadMutation.mutate,
        deleteFile: deleteMutation.mutate,
        isUploading: uploadMutation.isPending,
        isDeleting: deleteMutation.isPending,
    };
}
