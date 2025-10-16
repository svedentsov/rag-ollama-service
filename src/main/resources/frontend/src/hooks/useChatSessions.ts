import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
import { ChatSession } from '../types';
import { useRouter } from './useRouter';
import { useSessionStore } from '../state/sessionStore';

const CHAT_SESSIONS_QUERY_KEY = ['chatSessions'];

/**
 * @description Хук для управления сессиями чата. Инкапсулирует всю логику
 * взаимодействия с API сессий (получение, создание, удаление, переименование)
 * и управляет состоянием кэша React Query и глобального UI-стора `useSessionStore`.
 *
 * @returns {{
 *   sessions: ChatSession[],
 *   createChat: () => Promise<ChatSession>,
 *   deleteChat: (sessionId: string) => Promise<void>,
 *   renameChat: (vars: { sessionId: string, newName: string }) => Promise<void>,
 *   setActiveBranch: (vars: { sessionId: string, parentId: string, childId: string }) => void
 * } & Omit<ReturnType<typeof useQuery>, 'data'>} Объект с данными о сессиях и функциями-мутациями.
 */
export function useChatSessions() {
    const queryClient = useQueryClient();
    const { navigate, sessionId: currentSessionId } = useRouter();
    const { setSessions, setActiveBranch: setActiveBranchInStore, deleteSessionState } = useSessionStore();

    const { data: sessions = [], ...queryInfo } = useQuery<ChatSession[]>({
        queryKey: CHAT_SESSIONS_QUERY_KEY,
        queryFn: api.getChatSessions,
        // ИСПРАВЛЕНИЕ: Синхронизация состояния перенесена в onSuccess.
        // React Query гарантирует, что этот колбэк не будет вызван для размонтированного компонента.
        onSuccess: (data) => {
            setSessions(data);
        },
    });

    const createChatMutation = useMutation({
        mutationFn: api.createNewChat,
        onSuccess: (newChat) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            navigate(newChat.sessionId);
            return newChat;
        },
    });

    const deleteChatMutation = useMutation({
        mutationFn: api.deleteChatSession,
        onSuccess: (_, sessionIdToDelete) => {
            deleteSessionState(sessionIdToDelete);
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            if (currentSessionId === sessionIdToDelete) {
                navigate(null);
            }
        },
    });

    const renameChatMutation = useMutation({
        mutationFn: api.updateChatName,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
        },
    });

    const setActiveBranchMutation = useMutation({
        mutationFn: api.setActiveBranch,
        onMutate: async ({ sessionId, parentId, childId }) => {
            setActiveBranchInStore(sessionId, parentId, childId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
        },
    });

    return {
        sessions,
        ...queryInfo,
        createChat: createChatMutation.mutateAsync,
        deleteChat: deleteChatMutation.mutateAsync,
        renameChat: renameChatMutation.mutateAsync,
        setActiveBranch: setActiveBranchMutation.mutate,
    };
}
