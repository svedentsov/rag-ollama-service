import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
import { Message, ServerMessageDto } from '../types';

/**
 * Чистая функция-маппер для преобразования серверного DTO в клиентскую модель сообщения.
 * @param {ServerMessageDto} dto - Data Transfer Object с сервера.
 * @returns {Message} Клиентская модель сообщения.
 */
const transformMessageDtoToModel = (dto: ServerMessageDto): Message => ({
  id: dto.id,
  taskId: dto.taskId,
  parentId: dto.parentId ?? undefined,
  type: dto.role === 'USER' ? 'user' : 'assistant',
  text: dto.content,
  createdAt: dto.createdAt,
  sources: dto.sourceCitations,
  queryFormationHistory: dto.queryFormationHistory,
  finalPrompt: dto.finalPrompt,
  trustScoreReport: dto.trustScoreReport,
  isStreaming: false,
});

/**
 * Хук для управления сообщениями в конкретной сессии чата.
 * Инкапсулирует логику получения, трансформации, оптимистичного обновления и удаления сообщений.
 * @param {string} sessionId - ID сессии чата, для которой нужно управлять сообщениями.
 * @returns {object} Объект, содержащий:
 * - `messages`: Массив сообщений в клиентском формате.
 * - `...queryInfo`: Остальные свойства из `useQuery` (isLoading, error и т.д.).
 * - `updateMessage`: Функция-мутация для обновления сообщения.
 * - `deleteMessage`: Функция-мутация для удаления сообщения.
 */
export function useChatMessages(sessionId: string) {
    const queryClient = useQueryClient();
    const queryKey = ['messages', sessionId];

    const { data: messages = [], ...queryInfo } = useQuery<Message[]>({
        queryKey,
        queryFn: async () => {
            const serverMessages = await api.fetchMessages(sessionId);
            return serverMessages
                .map(transformMessageDtoToModel)
                .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        },
        enabled: !!sessionId,
    });

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

    const deleteMessageMutation = useMutation({
        mutationFn: api.deleteMessage,
        onMutate: async (messageId: string) => {
            await queryClient.cancelQueries({ queryKey });
            const previousMessages = queryClient.getQueryData<Message[]>(queryKey) ?? [];

            const idsToDelete = new Set<string>([messageId]);
            let newChildrenFound = true;
            while(newChildrenFound) {
                newChildrenFound = false;
                previousMessages.forEach(msg => {
                    if (msg.parentId && idsToDelete.has(msg.parentId) && !idsToDelete.has(msg.id)) {
                        idsToDelete.add(msg.id);
                        newChildrenFound = true;
                    }
                });
            }

            queryClient.setQueryData<Message[]>(queryKey, old =>
                old?.filter(msg => !idsToDelete.has(msg.id))
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
