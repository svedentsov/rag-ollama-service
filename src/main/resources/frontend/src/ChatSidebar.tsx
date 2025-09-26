import React, { useState, useEffect, useRef } from 'react';
import { Nav, Button, Spinner, Form } from 'react-bootstrap';
import { ChatSession } from './types';
import { fetchChatSessions, createNewChat, deleteChatSession, updateChatName } from './api';
import TrashIcon from './assets/trash.svg?react';
import EditIcon from './assets/edit.svg?react';

interface ContextMenuState {
    show: boolean;
    x: number;
    y: number;
    session: ChatSession | null;
}

interface ChatSidebarProps {
    currentSessionId: string | null;
}

function formatTimestamp(dateString?: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffSeconds = Math.round((now.getTime() - date.getTime()) / 1000);

    if (diffSeconds < 60) return `${diffSeconds}s ago`;
    const diffMinutes = Math.round(diffSeconds / 60);
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    const diffHours = Math.round(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours}h ago`;

    return date.toLocaleDateString();
}

export function ChatSidebar({ currentSessionId }: ChatSidebarProps) {
    const [sessions, setSessions] = useState<ChatSession[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
    const [editingName, setEditingName] = useState('');
    const [rightClickState, setRightClickState] = useState<{ sessionId: string | null, count: number }>({ sessionId: null, count: 0 });
    const [contextMenu, setContextMenu] = useState<ContextMenuState>({ show: false, x: 0, y: 0, session: null });
    const editInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        const loadSessions = async () => {
            try {
                setIsLoading(true);
                setSessions(await fetchChatSessions());
            } catch (error) { console.error("Failed to load chat sessions:", error); }
            finally { setIsLoading(false); }
        };
        loadSessions();

        const handleClickAndContextMenu = () => {
            setContextMenu(prev => ({ ...prev, show: false }));
        };
        document.addEventListener("click", handleClickAndContextMenu);
        // Используем фазу захвата, чтобы этот листенер сработал до всплытия нашего кастомного
        document.addEventListener("contextmenu", handleClickAndContextMenu, true);

        return () => {
            document.removeEventListener("click", handleClickAndContextMenu);
            document.removeEventListener("contextmenu", handleClickAndContextMenu, true);
        };
    }, []);

    useEffect(() => {
        if (editingSessionId && editInputRef.current) {
            editInputRef.current.focus();
            editInputRef.current.select();
        }
    }, [editingSessionId]);

    const handleNewChat = () => createNewChat().then(chat => window.location.href = `/chat?sessionId=${chat.sessionId}`).catch(console.error);

    const handleDelete = (session: ChatSession) => {
        if (window.confirm('Вы уверены, что хотите удалить этот чат?')) {
            deleteChatSession(session.sessionId).then(() => {
                setSessions(prev => prev.filter(s => s.sessionId !== session.sessionId));
                if (session.sessionId === currentSessionId) window.location.href = '/';
            }).catch(() => alert("Не удалось удалить чат."));
        }
    };

    const handleRename = (session: ChatSession) => {
        setEditingSessionId(session.sessionId);
        setEditingName(session.chatName);
    };

    const handleNameSubmit = async (e: React.FormEvent, sessionId: string) => {
        e.preventDefault();
        const originalName = sessions.find(s => s.sessionId === sessionId)?.chatName || '';
        if (editingName.trim() === '' || editingName === originalName) {
            setEditingSessionId(null);
            return;
        }
        try {
            await updateChatName(sessionId, editingName);
            setSessions(prev => prev.map(s => s.sessionId === sessionId ? { ...s, chatName: editingName } : s));
        } catch (error) {
            alert("Не удалось переименовать чат.");
        } finally {
            setEditingSessionId(null);
        }
    };

    const handleContextMenu = (e: React.MouseEvent, session: ChatSession) => {
        const isSameElement = rightClickState.sessionId === session.sessionId;
        const newCount = isSameElement ? rightClickState.count + 1 : 1;

        if (newCount >= 2) {
            setRightClickState({ sessionId: null, count: 0 });
            return;
        }

        e.preventDefault();
        e.stopPropagation();
        setContextMenu({ show: true, x: e.clientX, y: e.clientY, session });
        setRightClickState({ sessionId: session.sessionId, count: 1 });
    };

    const handleLeftClick = () => {
        if (rightClickState.count > 0) {
            setRightClickState({ sessionId: null, count: 0 });
        }
    };

    return (
        <>
            <nav className="d-flex flex-column p-2 bg-light border-end h-100 sidebar-nav">
                <div className="p-2 mb-2">
                    <Button variant="outline-secondary" className="w-100" onClick={handleNewChat}>+ Новый чат</Button>
                </div>
                {isLoading ? (
                    <div className="text-center p-4"><Spinner animation="border" size="sm" /></div>
                ) : (
                    <Nav variant="pills" className="flex-column mb-auto gap-1">
                        {sessions.map(session => (
                            <Nav.Item key={session.sessionId}>
                                <Nav.Link
                                    href={`/chat?sessionId=${session.sessionId}`}
                                    active={session.sessionId === currentSessionId && !editingSessionId}
                                    className="d-flex flex-column align-items-start"
                                    onContextMenu={(e) => handleContextMenu(e, session)}
                                    onClick={handleLeftClick}
                                >
                                    <div className="chat-title-container">
                                        {editingSessionId === session.sessionId ? (
                                            <Form onSubmit={(e) => handleNameSubmit(e, session.sessionId)} className="w-100">
                                                <Form.Control
                                                    ref={editInputRef} size="sm" value={editingName}
                                                    onChange={(e) => setEditingName(e.target.value)}
                                                    onBlur={(e) => handleNameSubmit(e as any, session.sessionId)}
                                                    onClick={(e) => { e.preventDefault(); e.stopPropagation(); }}
                                                    onContextMenu={(e) => e.stopPropagation()}
                                                />
                                            </Form>
                                        ) : (
                                            <span className="chat-title">{session.chatName}</span>
                                        )}
                                    </div>
                                    <div className="w-100 d-flex justify-content-between">
                                        <span className="chat-snippet">{session.lastMessageContent || 'Нет сообщений...'}</span>
                                        <span className="chat-meta">{formatTimestamp(session.lastMessageTimestamp)}</span>
                                    </div>
                                </Nav.Link>
                            </Nav.Item>
                        ))}
                    </Nav>
                )}
            </nav>

            {contextMenu.show && contextMenu.session && (
                <div className="custom-context-menu" style={{ top: contextMenu.y, left: contextMenu.x }}>
                    <button className="custom-context-menu-item" onClick={() => handleRename(contextMenu.session!)}>
                        <EditIcon className="me-1" /> Переименовать
                    </button>
                    <button className="custom-context-menu-item danger" onClick={() => handleDelete(contextMenu.session!)}>
                        <TrashIcon className="me-1" /> Удалить
                    </button>
                </div>
            )}
        </>
    );
}