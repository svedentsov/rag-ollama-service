import React, { useState, useEffect, useRef, useMemo } from 'react';
import toast from 'react-hot-toast';
import { ChatSession } from './types';
import { useChatSessions } from './hooks/useChatSessions';
import { useDebounce } from './hooks/useDebounce';
import { useRouter } from './hooks/useRouter';
import { useNotifications, useClearNotification } from './state/notificationStore';
import { SearchInput } from './components/SearchInput';
import { NotificationDot } from './components/NotificationDot';
import { Trash, Edit, Plus } from 'lucide-react';
import styles from './ChatSidebar.module.css';

/**
 * @internal
 * Состояние для контекстного меню.
 */
interface ContextMenuState {
  show: boolean;
  x: number;
  y: number;
  session: ChatSession | null;
}

/**
 * Пропсы для компонента ChatSidebar.
 */
interface ChatSidebarProps {
  /** @param currentSessionId - ID активной в данный момент сессии чата. */
  currentSessionId: string | null;
}

/**
 * Компонент боковой панели, отображающий список сессий чата,
 * управляющий поиском, созданием, удалением и переименованием чатов,
 * а также отображающий уведомления о новых сообщениях.
 * @param {ChatSidebarProps} props - Пропсы компонента.
 */
export function ChatSidebar({ currentSessionId }: ChatSidebarProps) {
  const { sessions, isLoading, createChat, deleteChat, renameChat } = useChatSessions();
  const { navigate } = useRouter();
  const { notifications } = useNotifications();
  const clearNotification = useClearNotification();

  const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState('');
  const [contextMenu, setContextMenu] = useState<ContextMenuState>({ show: false, x: 0, y: 0, session: null });
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 300);
  const editInputRef = useRef<HTMLInputElement>(null);

  const filteredSessions = useMemo(() => {
    if (!debouncedSearchTerm) return sessions;
    return sessions.filter(session =>
      session.chatName.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
    );
  }, [sessions, debouncedSearchTerm]);

  // Закрывает контекстное меню при клике в любом месте
  useEffect(() => {
    const handleClick = () => setContextMenu(prev => ({ ...prev, show: false }));
    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, []);

  // Фокусируется на поле ввода при начале редактирования
  useEffect(() => {
    if (editingSessionId && editInputRef.current) {
      editInputRef.current.focus();
      editInputRef.current.select();
    }
  }, [editingSessionId]);

  const handleNewChat = () => {
    toast.promise(createChat(), {
      loading: 'Создание чата...',
      success: 'Чат создан!',
      error: 'Ошибка при создании чата.',
    });
  };

  const handleDelete = (session: ChatSession) => {
    if (window.confirm(`Вы уверены, что хотите удалить чат "${session.chatName}"?`)) {
      toast.promise(deleteChat(session.sessionId), {
        loading: 'Удаление...',
        success: 'Чат удален!',
        error: 'Ошибка при удалении.',
      });
    }
  };

  const handleRename = (session: ChatSession) => {
    setEditingSessionId(session.sessionId);
    setEditingName(session.chatName);
  };

  const handleNameSubmit = async (e: React.FormEvent, session: ChatSession) => {
    e.preventDefault();
    const originalName = sessions.find(s => s.sessionId === session.sessionId)?.chatName || '';
    setEditingSessionId(null);
    if (editingName.trim() && editingName !== originalName) {
      toast.promise(renameChat({ sessionId: session.sessionId, newName: editingName }), {
        loading: 'Переименование...',
        success: 'Чат переименован.',
        error: 'Ошибка переименования.',
      });
    }
  };

  const handleContextMenu = (e: React.MouseEvent, session: ChatSession) => {
    e.preventDefault();
    e.stopPropagation();
    setContextMenu({ show: true, x: e.clientX, y: e.clientY, session });
  };

  const handleNavigation = (e: React.MouseEvent, sessionId: string) => {
    e.preventDefault();
    if (notifications.has(sessionId)) {
      clearNotification(sessionId);
    }
    navigate(sessionId);
  };

  return (
    <>
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
                  <button
                    onClick={(e) => handleNavigation(e, session.sessionId)}
                    onContextMenu={(e) => handleContextMenu(e, session)}
                    className={`${styles.navLink} ${session.sessionId === currentSessionId && !editingSessionId ? styles.active : ''}`}
                  >
                    {editingSessionId === session.sessionId ? (
                      <form onSubmit={(e) => handleNameSubmit(e, session)} className={styles.renameForm}>
                        <input
                          ref={editInputRef}
                          type="text"
                          value={editingName}
                          onChange={(e) => setEditingName(e.target.value)}
                          onBlur={(e) => handleNameSubmit(e, session)}
                          onClick={(e) => { e.preventDefault(); e.stopPropagation(); }}
                          className={styles.renameInput}
                          aria-label={`Новое имя для чата ${session.chatName}`}
                        />
                      </form>
                    ) : (
                      <>
                        <span className={styles.chatTitle}>{session.chatName}</span>
                        {notifications.has(session.sessionId) && <NotificationDot />}
                      </>
                    )}
                  </button>
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

      {contextMenu.show && contextMenu.session && (
        <div className={styles.contextMenu} style={{ top: contextMenu.y, left: contextMenu.x }} role="menu">
          <button className={styles.contextMenuItem} onClick={() => handleRename(contextMenu.session!)} role="menuitem">
            <Edit size={14} aria-hidden="true" /> Переименовать
          </button>
          <button className={`${styles.contextMenuItem} ${styles.danger}`} onClick={() => handleDelete(contextMenu.session!)} role="menuitem">
            <Trash size={14} aria-hidden="true" /> Удалить
          </button>
        </div>
      )}
    </>
  );
}
