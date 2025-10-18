import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { api } from '../../api';
import { FileDto, Page } from '../../types';

const FILES_QUERY_KEY = 'files';

/**
 * @typedef SortOptions
 * @description Определяет параметры сортировки для списка файлов.
 * @property {string} id - Поле для сортировки.
 * @property {boolean} desc - Направление сортировки (true для DESC).
 */
type SortOptions = {
    id: string;
    desc: boolean;
};

/**
 * Хук для управления серверным состоянием списка файлов.
 * Отвечает за получение, пагинацию, сортировку, фильтрацию и удаление файлов.
 * @param page - Номер текущей страницы.
 * @param size - Размер страницы.
 * @param sort - Параметры сортировки.
 * @param searchTerm - Поисковый запрос для фильтрации.
 * @returns Объект, содержащий данные страницы, статус загрузки и функции для мутаций.
 */
export function useFileList(page: number, size: number, sort: SortOptions, searchTerm: string) {
    const queryClient = useQueryClient();
    const queryKey = [FILES_QUERY_KEY, page, size, sort, searchTerm];

    const { data, isLoading, error } = useQuery<Page<FileDto>>({
        queryKey,
        queryFn: () => api.getFiles({ page, size, sort: sort.id, direction: sort.desc ? 'DESC' : 'ASC', query: searchTerm }),
    });

    const deleteMutation = useMutation({
        mutationFn: (fileIds: string[]) => api.deleteFiles(fileIds),
        onSuccess: (_, fileIds) => {
            toast.success(`Удалено файлов: ${fileIds.length}`);
        },
        onError: () => toast.error('Не удалось удалить файлы.'),
        onSettled: () => queryClient.invalidateQueries({ queryKey: [FILES_QUERY_KEY] }),
    });

    return {
        pageData: data,
        isLoading,
        error: error as Error | null,
        deleteFiles: deleteMutation.mutate,
        isDeleting: deleteMutation.isPending,
    };
}
