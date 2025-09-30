import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as api from '../api';
import { ChatSession } from '../types';
import { useRouter } from './useRouter';

const CHAT_SESSIONS_QUERY_KEY = ['chatSessions'];

/**
 * Хук для управления сессиями чата.
 * Инкапсулирует логику запроса списка сессий, создания, переименования и удаления,
 * используя TanStack Query для управления кэшем и состоянием сервера.
 * Использует `useRouter` для навигации без перезагрузки страницы.
 */
export function useChatSessions() {
    const queryClient = useQueryClient();
    const { navigate, sessionId: currentSessionId } = useRouter();

    /**
     * Запрос на получение списка всех сессий чата.
     */
    const { data: sessions = [], ...queryInfo } = useQuery<ChatSession[]>({
        queryKey: CHAT_SESSIONS_QUERY_KEY,
        queryFn: api.fetchChatSessions,
    });

    /**
     * Мутация для создания новой сессии чата.
     */
    const createChatMutation = useMutation({
        mutationFn: api.createNewChat,
        onSuccess: (newChat) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            navigate(newChat.sessionId);
        },
    });

    /**
     * Мутация для удаления сессии чата.
     */
    const deleteChatMutation = useMutation({
        mutationFn: api.deleteChatSession,
        onSuccess: (_, sessionIdToDelete) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            if (currentSessionId === sessionIdToDelete) {
                navigate(null); // Переход на главную страницу, если удален текущий чат
            }
        },
    });

    /**
     * Мутация для переименования сессии чата.
     */
    const renameChatMutation = useMutation({
        mutationFn: api.updateChatName,
        onSuccess: (_, { sessionId, newName }) => {
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, (oldData) =>
                oldData?.map(session =>
                    session.sessionId === sessionId ? { ...session, chatName: newName } : session
                ) ?? []
            );
        },
        onError: (_, { sessionId }) => {
            queryClient.invalidateQueries({ queryKey: [CHAT_SESSIONS_QUERY_KEY, sessionId] });
        }
    });

    return {
        sessions,
        ...queryInfo,
        createChat: createChatMutation.mutateAsync,
        deleteChat: deleteChatMutation.mutateAsync,
        renameChat: renameChatMutation.mutateAsync,
    };
}
