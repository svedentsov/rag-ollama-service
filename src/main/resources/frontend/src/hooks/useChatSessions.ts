import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { api } from '../api';
import { ChatSession } from '../types';
import { useRouter } from './useRouter';
import { useSessionStore } from '../state/sessionStore';

const CHAT_SESSIONS_QUERY_KEY = ['chatSessions'];

/**
 * Хук для управления сессиями чата с реализацией оптимистичных обновлений
 * и централизованной обработкой обратной связи (toast).
 * @returns Объект с данными о сессиях и отказоустойчивыми функциями-мутациями.
 */
export function useChatSessions() {
    const queryClient = useQueryClient();
    const { navigate, sessionId: currentSessionId } = useRouter();
    const { setSessions, setActiveBranch: setActiveBranchInStore, deleteSessionState } = useSessionStore();

    const { data: sessions = [], ...queryInfo } = useQuery<ChatSession[]>({
        queryKey: CHAT_SESSIONS_QUERY_KEY,
        queryFn: api.getChatSessions,
        onSuccess: (data) => {
            setSessions(data);
        },
    });

    const createChatMutation = useMutation({
        mutationFn: api.createNewChat,
        onSuccess: (newChat) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            navigate(`/chat?sessionId=${newChat.sessionId}`);
            toast.success('Чат успешно создан!');
        },
        onError: (error: Error) => {
            toast.error(`Ошибка при создании чата: ${error.message}`);
        }
    });

    const deleteChatMutation = useMutation({
        mutationFn: api.deleteChatSession,
        onSuccess: (_, sessionIdToDelete) => {
            deleteSessionState(sessionIdToDelete);
            toast.success('Чат удален!');
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            if (currentSessionId === sessionIdToDelete) {
                navigate(null);
            }
        },
        onError: (error: Error) => {
            toast.error(`Ошибка при удалении чата: ${error.message}`);
        }
    });

    const renameChatMutation = useMutation({
        mutationFn: api.updateChatName,
        onMutate: async ({ sessionId, newName }) => {
            await queryClient.cancelQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            const previousSessions = queryClient.getQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY) ?? [];
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, old =>
                old?.map(session => session.sessionId === sessionId ? { ...session, chatName: newName } : session)
            );
            return { previousSessions };
        },
        onSuccess: () => {
            toast.success('Чат успешно переименован.');
        },
        onError: (err: Error, variables, context) => {
            console.error("Ошибка переименования чата:", err);
            if (context?.previousSessions) {
                queryClient.setQueryData(CHAT_SESSIONS_QUERY_KEY, context.previousSessions);
            }
            toast.error(`Не удалось переименовать чат: ${err.message}`);
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
        },
    });

    const setActiveBranchMutation = useMutation({
        mutationFn: api.setActiveBranch,
        onMutate: async ({ sessionId, parentId, childId }) => {
            await queryClient.cancelQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            const previousSessions = queryClient.getQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY) ?? [];

            setActiveBranchInStore(sessionId, parentId, childId);
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, old =>
                old?.map(session => {
                    if (session.sessionId === sessionId) {
                        return { ...session, activeBranches: { ...(session.activeBranches || {}), [parentId]: childId } };
                    }
                    return session;
                })
            );
            return { previousSessions };
        },
        onError: (err: Error, variables, context) => {
            if (context?.previousSessions) {
                queryClient.setQueryData(CHAT_SESSIONS_QUERY_KEY, context.previousSessions);
                setSessions(context.previousSessions);
            }
            toast.error(`Ошибка при выборе ветки: ${err.message}`);
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
        },
    });

    return {
        sessions,
        ...queryInfo,
        createChat: createChatMutation.mutate,
        deleteChat: deleteChatMutation.mutate,
        renameChat: renameChatMutation.mutate,
        setActiveBranch: setActiveBranchMutation.mutate,
    };
}
