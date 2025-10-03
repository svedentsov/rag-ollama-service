import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api';
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
        queryFn: api.getChatSessions,
    });

    /**
     * Мутация для создания новой сессии чата с применением оптимистичного обновления.
     */
    const createChatMutation = useMutation({
        mutationFn: api.createNewChat,
        // Оптимистичное обновление: немедленно обновляем UI, не дожидаясь ответа сервера.
        onMutate: async () => {
            // Отменяем все активные запросы на получение списка чатов
            await queryClient.cancelQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            // Сохраняем текущее состояние на случай, если придется откатываться
            const previousSessions = queryClient.getQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY) ?? [];
            
            // Создаем "фейковый" новый чат для немедленного отображения
            const optimisticChat: ChatSession = {
                sessionId: `optimistic-${Date.now()}`, // Временный уникальный ID
                chatName: 'Новый чат...',
                lastMessageTimestamp: new Date().toISOString(),
            };

            // Вручную обновляем кэш, добавляя наш оптимистичный чат в начало списка
            queryClient.setQueryData<ChatSession[]>(CHAT_SESSIONS_QUERY_KEY, [optimisticChat, ...previousSessions]);
            
            // Немедленно переходим на страницу нового чата
            navigate(optimisticChat.sessionId);

            return { previousSessions };
        },
        // onSuccess: Мы больше не делаем навигацию здесь, так как она уже произошла оптимистично.
        // Вместо этого мы могли бы обновить наш 'optimistic' чат реальными данными с сервера,
        // но для простоты и надежности мы просто инвалидируем кэш.
        onError: (err, variables, context) => {
            // В случае ошибки откатываем кэш к состоянию до мутации.
            if (context?.previousSessions) {
                queryClient.setQueryData(CHAT_SESSIONS_QUERY_KEY, context.previousSessions);
            }
            // Возвращаемся на главную страницу, так как создание чата не удалось
            navigate(null);
        },
        // onSettled: Выполняется всегда - и после успеха, и после ошибки.
        // Гарантирует, что наш UI в итоге будет синхронизирован с состоянием сервера.
        onSettled: (newChat) => {
            queryClient.invalidateQueries({ queryKey: CHAT_SESSIONS_QUERY_KEY });
            // Если сервер вернул реальный чат, и мы все еще на временной странице,
            // обновляем URL на правильный, серверный ID.
            if (newChat && currentSessionId?.startsWith('optimistic-')) {
                navigate(newChat.sessionId);
            }
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
                navigate(null);
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
