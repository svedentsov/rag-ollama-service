import React, { useState, useMemo } from 'react';
import toast from 'react-hot-toast';
import { useChatSessions } from './hooks/useChatSessions';
import { useDebounce } from './hooks/useDebounce';
import { useRouter } from './hooks/useRouter';
import { useNotificationStore } from './state/notificationStore';
import { SearchInput } from './components/SearchInput';
import { ChatListItem } from './components/ChatListItem';
import { Plus } from 'lucide-react';
import styles from './ChatSidebar.module.css';

/**
 * @interface ChatSidebarProps
 * @description Пропсы для компонента ChatSidebar.
 */
interface ChatSidebarProps {
  /** @param {string | null} currentSessionId - ID активной в данный момент сессии чата. */
  currentSessionId: string | null;
}

/**
 * Компонент боковой панели для навигации по чатам.
 * Управляет состоянием поиска, отображает список чатов и кнопку создания нового.
 * @param {ChatSidebarProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент боковой панели.
 */
export function ChatSidebar({ currentSessionId }: ChatSidebarProps) {
  const { sessions, isLoading, createChat, deleteChat, renameChat } = useChatSessions();
  const { navigate } = useRouter();
  const { notifications, clearNotification } = useNotificationStore();
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 300);

  const filteredSessions = useMemo(() => {
    if (!debouncedSearchTerm) return sessions;
    return sessions.filter(session =>
      session.chatName.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
    );
  }, [sessions, debouncedSearchTerm]);

  const handleNewChat = () => {
    toast.promise(createChat(), {
      loading: 'Создание чата...',
      success: 'Чат создан!',
      error: 'Ошибка при создании чата.',
    });
  };

  const handleNavigation = (sessionId: string) => {
    if (notifications.has(sessionId)) {
      clearNotification(sessionId);
    }
    navigate(sessionId);
  };

  return (
    <nav className={styles.sidebar}>
      <div className={styles.sidebarHeader}>
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Поиск по чатам..."
          ariaLabel="Поиск по чатам"
        />
      </div>
      <div className={styles.sidebarContent}>
        {isLoading && <div className={styles.centered}><div className={styles.spinner} role="status" aria-label="Загрузка чатов"></div></div>}
        {!isLoading && filteredSessions.length === 0 && (
          <div className={styles.emptyState}>
            <small>{searchTerm ? 'Чаты не найдены.' : 'История чатов пуста.'}</small>
          </div>
        )}
        {!isLoading && (
          <ul className={styles.navList}>
            {filteredSessions.map(session => (
              <li key={session.sessionId}>
                <ChatListItem
                  session={session}
                  isActive={session.sessionId === currentSessionId}
                  hasNotification={notifications.has(session.sessionId)}
                  onNavigate={handleNavigation}
                  onRename={(newName) => toast.promise(renameChat({ sessionId: session.sessionId, newName }), {
                    loading: 'Переименование...',
                    success: 'Чат переименован.',
                    error: 'Ошибка переименования.',
                  })}
                  onDelete={() => {
                    if (window.confirm(`Вы уверены, что хотите удалить чат "${session.chatName}"?`)) {
                      toast.promise(deleteChat(session.sessionId), {
                        loading: 'Удаление...',
                        success: 'Чат удален!',
                        error: 'Ошибка при удалении.',
                      });
                    }
                  }}
                />
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className={styles.sidebarFooter}>
        <button className={styles.newChatButton} onClick={handleNewChat}>
          <Plus size={16} aria-hidden="true" /> <span>Новый чат</span>
        </button>
      </div>
    </nav>
  );
}
