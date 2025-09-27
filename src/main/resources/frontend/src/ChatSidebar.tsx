import React, { useState, useEffect, useRef, useMemo } from 'react';
import toast from 'react-hot-toast';
import { ChatSession } from './types';
import { useChatSessions } from './hooks/useChatSessions';
import { Trash, Edit, Plus, Search } from 'lucide-react';
import styles from './ChatSidebar.module.css';

interface ContextMenuState { show: boolean; x: number; y: number; session: ChatSession | null; }
interface ChatSidebarProps { currentSessionId: string | null; }

export function ChatSidebar({ currentSessionId }: ChatSidebarProps) {
    const { sessions, isLoading, createChat, deleteChat, renameChat } = useChatSessions();
    const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
    const [editingName, setEditingName] = useState('');
    const [contextMenu, setContextMenu] = useState<ContextMenuState>({ show: false, x: 0, y: 0, session: null });
    const [searchTerm, setSearchTerm] = useState('');
    const editInputRef = useRef<HTMLInputElement>(null);

    const filteredSessions = useMemo(() => {
        if (!searchTerm) return sessions;
        return sessions.filter(session =>
            session.chatName.toLowerCase().includes(searchTerm.toLowerCase())
        );
    }, [sessions, searchTerm]);

    useEffect(() => {
        const handleClick = () => setContextMenu(prev => ({ ...prev, show: false }));
        document.addEventListener('click', handleClick);
        return () => document.removeEventListener('click', handleClick);
    }, []);

    useEffect(() => {
        if (editingSessionId && editInputRef.current) {
            editInputRef.current.focus();
            editInputRef.current.select();
        }
    }, [editingSessionId]);

    const handleNewChat = () => toast.promise(Promise.resolve(createChat()), { loading: 'Создание чата...', success: 'Чат создан!', error: 'Ошибка.' });
    const handleDelete = (session: ChatSession) => {
        if (window.confirm(`Вы уверены, что хотите удалить чат "${session.chatName}"?`)) {
            toast.promise(Promise.resolve(deleteChat(session.sessionId)), { loading: 'Удаление...', success: 'Чат удален!', error: 'Ошибка.' });
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
            renameChat({ sessionId: session.sessionId, newName: editingName });
            toast.success('Чат переименован.');
        }
    };

    const handleContextMenu = (e: React.MouseEvent, session: ChatSession) => {
        e.preventDefault();
        e.stopPropagation();
        setContextMenu({ show: true, x: e.clientX, y: e.clientY, session });
    };

    return (
        <>
            <nav className={styles.sidebar}>
                <div className={styles.sidebarHeader}>
                    <div className={styles.searchWrapper}>
                        <Search className={styles.searchIcon} size={16} />
                        <input
                            type="text"
                            placeholder="Поиск..."
                            className={styles.searchInput}
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>
                <div className={styles.sidebarContent}>
                    {isLoading && <div className={styles.centered}><div className={styles.spinner}></div></div>}
                    {!isLoading && filteredSessions.length === 0 && (
                        <div className={styles.emptyState}>
                            <small>{searchTerm ? 'Чаты не найдены.' : 'История чатов пуста.'}</small>
                        </div>
                    )}
                    {!isLoading && (
                        <ul className={styles.navList}>
                            {filteredSessions.map(session => (
                                <li key={session.sessionId}>
                                    <a href={`/chat?sessionId=${session.sessionId}`} className={`${styles.navLink} ${session.sessionId === currentSessionId && !editingSessionId ? styles.active : ''}`} onContextMenu={(e) => handleContextMenu(e, session)}>
                                        {editingSessionId === session.sessionId ? (
                                            <form onSubmit={(e) => handleNameSubmit(e, session)} className={styles.renameForm}>
                                                <input ref={editInputRef} type="text" value={editingName} onChange={(e) => setEditingName(e.target.value)} onBlur={(e) => handleNameSubmit(e as any, session)} onClick={(e) => { e.preventDefault(); e.stopPropagation(); }} className={styles.renameInput} />
                                            </form>
                                        ) : (
                                            <span className={styles.chatTitle}>{session.chatName}</span>
                                        )}
                                    </a>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
                <div className={styles.sidebarFooter}>
                    <button className={styles.newChatButton} onClick={handleNewChat}>
                        <Plus size={16} /> <span>Новый чат</span>
                    </button>
                </div>
            </nav>

            {contextMenu.show && contextMenu.session && (
                <div className={styles.contextMenu} style={{ top: contextMenu.y, left: contextMenu.x }}>
                    <button className={styles.contextMenuItem} onClick={() => handleRename(contextMenu.session!)}><Edit size={14}/> Переименовать</button>
                    <button className={`${styles.contextMenuItem} ${styles.danger}`} onClick={() => handleDelete(contextMenu.session!)}><Trash size={14}/> Удалить</button>
                </div>
            )}
        </>
    );
}
