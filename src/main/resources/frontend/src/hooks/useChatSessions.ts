import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../api';
import { ChatSession } from '../types';

const CHAT_SESSIONS_QUERY_KEY = 'chatSessions';

/**
 * Хук для управления сессиями чата.
 * Инкапсулирует логику запроса списка сессий, создания, переименования и удаления,
 * используя TanStack Query для управления кэшем и состоянием сервера.
 */
export function useChatSessions() {
    const queryClient = useQueryClient();

    /**
     * Запрос на получение списка всех сессий чата.
     */
    const { data: sessions = [], isLoading } = useQuery<ChatSession[]>({
        queryKey: [CHAT_SESSIONS_QUERY_KEY],
        queryFn: api.fetchChatSessions,
    });

    /**
     * Мутация для создания новой сессии чата.
     */
    const { mutate: createChat } = useMutation({
        mutationFn: api.createNewChat,
        onSuccess: (newChat) => {
            // Перенаправляем пользователя на страницу нового чата
            window.location.href = `/chat?sessionId=${newChat.sessionId}`;
            // Инвалидируем кэш, чтобы список чатов обновился
            queryClient.invalidateQueries({ queryKey: [CHAT_SESSIONS_QUERY_KEY] });
        },
    });

    /**
     * Мутация для удаления сессии чата.
     */
    const { mutate: deleteChat } = useMutation({
        mutationFn: api.deleteChatSession,
        onSuccess: (_, sessionIdToDelete) => {
            queryClient.invalidateQueries({ queryKey: [CHAT_SESSIONS_QUERY_KEY] });
            const params = new URLSearchParams(window.location.search);
            if (params.get('sessionId') === sessionIdToDelete) {
                window.location.href = '/';
            }
        },
    });

    /**
     * Мутация для переименования сессии чата.
     */
    const { mutate: renameChat } = useMutation({
        mutationFn: api.updateChatName,
        onSuccess: (_, { sessionId, newName }) => {
            // Оптимистично обновляем кэш для мгновенного отклика UI
            queryClient.setQueryData<ChatSession[]>([CHAT_SESSIONS_QUERY_KEY], (oldData) =>
                oldData?.map(session =>
                    session.sessionId === sessionId ? { ...session, chatName: newName } : session
                ) ?? []
            );
        },
        // При ошибке откатываем изменения
        onError: (_, { sessionId }) => {
            queryClient.invalidateQueries({ queryKey: [CHAT_SESSIONS_QUERY_KEY, sessionId] });
        }
    });

    return {
        sessions,
        isLoading,
        createChat,
        deleteChat,
        renameChat,
    };
}