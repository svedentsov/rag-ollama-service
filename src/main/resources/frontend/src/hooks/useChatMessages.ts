import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../api';
import { Message } from '../types';

/**
 * Хук для управления сообщениями в конкретной сессии чата.
 * Эта версия очищена от любой логики отображения и отвечает только за
 * взаимодействие с API и управление состоянием сервера через React Query.
 * @param sessionId - ID сессии чата.
 */
export function useChatMessages(sessionId: string) {
    const queryClient = useQueryClient();
    const queryKey = ['messages', sessionId];

    const { data: messages = [], ...queryInfo } = useQuery<Message[]>({
        queryKey,
        queryFn: () => api.fetchMessages(sessionId),
        enabled: !!sessionId,
    });

    /**
     * Мутация для обновления одного сообщения.
     * Реализует оптимистичное обновление для мгновенной обратной связи.
     */
    const updateMessageMutation = useMutation({
        mutationFn: api.updateMessage,
        onMutate: async ({ messageId, newContent }) => {
            await queryClient.cancelQueries({ queryKey });
            const previousMessages = queryClient.getQueryData<Message[]>(queryKey) ?? [];
            queryClient.setQueryData<Message[]>(queryKey, old =>
                old?.map(msg => msg.id === messageId ? { ...msg, text: newContent } : msg)
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

    /**
     * Мутация для удаления одного сообщения.
     * Реализует оптимистичное обновление.
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
