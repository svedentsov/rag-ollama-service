import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
import { ChatSession } from '../types';
import { useRouter } from './useRouter';

const CHAT_SESSIONS_QUERY_KEY = ['chatSessions'];

/**
 * Хук для управления сессиями чата.
 */
export function useChatSessions() {
    const queryClient = useQueryClient();
    const { navigate, sessionId: currentSessionId } = useRouter();

    const { data: sessions = [], ...queryInfo } = useQuery<ChatSession[]>({
        queryKey: CHAT_SESSIONS_QUERY_KEY,
        queryFn: api.getChatSessions,
    });

    const createChatMutation = useMutation({
        mutationFn: api.createNewChat,
        onSuccess: (newChat) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            navigate(newChat.sessionId);
        },
    });

    const deleteChatMutation = useMutation({
        mutationFn: api.deleteChatSession,
        onSuccess: (_, sessionIdToDelete) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            if (currentSessionId === sessionIdToDelete) {
                navigate(null);
            }
        },
    });

    const renameChatMutation = useMutation({
        mutationFn: api.updateChatName,
        onSuccess: (_, { sessionId, newName }) => {
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, (oldData) =>
                oldData?.map(session =>
                    session.sessionId === sessionId ? { ...session, chatName: newName } : session
                ) ?? []
            );
        },
    });

    /**
     * Мутация для установки активной ветки с оптимистичным обновлением.
     */
    const setActiveBranchMutation = useMutation({
        mutationFn: api.setActiveBranch,
        onMutate: async ({ sessionId, parentId, childId }) => {
            await queryClient.cancelQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            const previousSessions = queryClient.getQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY);
            
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, (oldData = []) =>
                oldData.map(session => {
                    if (session.sessionId === sessionId) {
                        return {
                            ...session,
                            activeBranches: {
                                ...session.activeBranches,
                                [parentId]: childId,
                            },
                        };
                    }
                    return session;
                })
            );
            return { previousSessions };
        },
        onError: (err, variables, context) => {
            if (context?.previousSessions) {
                queryClient.setQueryData(CHAT_SESSIONS_QUERY_KEY, context.previousSessions);
            }
        },
        onSettled: () => {
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
