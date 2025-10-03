import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
import { Message } from '../types';

/**
 * Хук для управления сообщениями в конкретной сессии чата.
 *
 * @description Этот хук инкапсулирует всю логику взаимодействия с API для сообщений,
 * используя React Query для управления состоянием сервера, кэшированием,
 * и оптимистичными обновлениями.
 *
 * @param {string} sessionId - ID сессии чата, для которой нужно получить и управлять сообщениями.
 * @returns {{
 *   messages: Message[];
 *   isLoading: boolean;
 *   isError: boolean;
 *   error: Error | null;
 *   updateMessage: (vars: { messageId: string; newContent: string; }) => void;
 *   deleteMessage: (messageId: string) => void;
 * }} Объект, содержащий массив сообщений, статусы загрузки/ошибки и функции-мутации для обновления и удаления.
 */
export function useChatMessages(sessionId: string) {
    const queryClient = useQueryClient();
    const queryKey = ['messages', sessionId];

    const { data: messages = [], ...queryInfo } = useQuery<Message[]>({
        queryKey,
        // ИСПОЛЬЗУЕМ КОРРЕКТНЫЙ ВЫЗОВ API
        queryFn: () => api.fetchMessages(sessionId),
        enabled: !!sessionId, // Запрос выполняется только если sessionId предоставлен
    });

    /**
     * Мутация для обновления одного сообщения.
     * Реализует оптимистичное обновление для мгновенной обратной связи в UI.
     */
    const updateMessageMutation = useMutation({
        mutationFn: api.updateMessage,
        onMutate: async ({ messageId, newContent }) => {
            // Отменяем все текущие запросы на получение сообщений, чтобы они не перезаписали наши оптимистичные данные
            await queryClient.cancelQueries({ queryKey });
            // Сохраняем предыдущее состояние на случай, если придется откатываться
            const previousMessages = queryClient.getQueryData<Message[]>(queryKey) ?? [];
            // Оптимистично обновляем UI, не дожидаясь ответа сервера
            queryClient.setQueryData<Message[]>(queryKey, old =>
                old?.map(msg => msg.id === messageId ? { ...msg, text: newContent } : msg)
            );
            return { previousMessages };
        },
        // В случае ошибки откатываемся к состоянию до мутации
        onError: (err, variables, context) => {
            if (context?.previousMessages) {
                queryClient.setQueryData(queryKey, context.previousMessages);
            }
        },
        // После завершения мутации (успешно или с ошибкой) всегда обновляем данные с сервера
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey });
        },
    });

    /**
     * Мутация для удаления одного сообщения.
     * Также реализует оптимистичное обновление.
     */
    const deleteMessageMutation = useMutation({
        mutationFn: api.deleteMessage,
        onMutate: async (messageId) => {
            await queryClient.cancelQueries({ queryKey });
            const previousMessages = queryClient.getQueryData<Message[]>(queryKey) ?? [];
            queryClient.setQueryData<Message[]>(queryKey, old =>
                old?.filter(msg => msg.id !== messageId)
            );
            return { previousMessages };
        },
        onError: (err, variables, context) => {
            if (context?.previousMessages) {
                queryClient.setQueryData(queryKey, context.previousMessages);
            }
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey });
        },
    });

    return {
        messages,
        ...queryInfo,
        updateMessage: updateMessageMutation.mutate,
        deleteMessage: deleteMessageMutation.mutate,
    };
}
