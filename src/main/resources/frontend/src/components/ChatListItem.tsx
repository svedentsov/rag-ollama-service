import React, { useRef, useState, FC, useEffect } from 'react';
import { ChatSession } from '../types';
import { useContextMenu } from '../hooks/useContextMenu';
import { NotificationDot } from './NotificationDot';
import { Trash, Edit } from 'lucide-react';
import styles from '../ChatSidebar.module.css';

/**
 * Пропсы для компонента ChatListItem.
 */
interface ChatListItemProps {
  /** @param session - Объект сессии чата для отображения. */
  session: ChatSession;
  /** @param isActive - Флаг, является ли данный чат активным в UI. */
  isActive: boolean;
  /** @param hasNotification - Флаг, есть ли для этого чата уведомление. */
  hasNotification: boolean;
  /** @param onNavigate - Колбэк для навигации к этому чату. */
  onNavigate: (sessionId: string) => void;
  /** @param onRename - Колбэк для переименования чата. */
  onRename: (newName: string) => void;
  /** @param onDelete - Колбэк для удаления чата. */
  onDelete: () => void;
}

/**
 * Отображает один элемент в списке чатов, инкапсулируя логику
 * редактирования, удаления и контекстного меню.
 * @param {ChatListItemProps} props - Пропсы компонента.
 */
export const ChatListItem: FC<ChatListItemProps> = ({
  session,
  isActive,
  hasNotification,
  onNavigate,
  onRename,
  onDelete,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editingName, setEditingName] = useState(session.chatName);
  const editInputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const { menuState, handleContextMenu, closeMenu } = useContextMenu<ChatSession>(menuRef);

  useEffect(() => {
    if (isEditing && editInputRef.current) {
      editInputRef.current.focus();
      editInputRef.current.select();
    }
  }, [isEditing]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsEditing(false);
    if (editingName.trim() && editingName !== session.chatName) {
      onRename(editingName.trim());
    }
  };

  const handleStartRename = () => {
    setIsEditing(true);
    setEditingName(session.chatName);
    closeMenu();
  };

  const handleDelete = () => {
    onDelete();
    closeMenu();
  };

  return (
    <>
      <button
        onClick={(e) => { e.preventDefault(); onNavigate(session.sessionId); }}
        onContextMenu={(e) => handleContextMenu(e, session)}
        className={`${styles.navLink} ${isActive && !isEditing ? styles.active : ''}`}
        aria-current={isActive ? 'page' : undefined}
      >
        {isEditing ? (
          <form onSubmit={handleSubmit} className={styles.renameForm}>
            <input
              ref={editInputRef}
              type="text"
              value={editingName}
              onChange={(e) => setEditingName(e.target.value)}
              onBlur={handleSubmit}
              onClick={(e) => e.stopPropagation()}
              className={styles.renameInput}
              aria-label={`Новое имя для чата ${session.chatName}`}
            />
          </form>
        ) : (
          <>
            <span className={styles.chatTitle}>{session.chatName}</span>
            {hasNotification && <NotificationDot />}
          </>
        )}
      </button>

      {menuState.show && menuState.item?.sessionId === session.sessionId && (
        <div ref={menuRef} className={styles.contextMenu} style={{ top: menuState.y, left: menuState.x }} role="menu" aria-label={`Действия для чата ${session.chatName}`}>
          <button className={styles.contextMenuItem} onClick={handleStartRename} role="menuitem">
            <Edit size={14} aria-hidden="true" /> Переименовать
          </button>
          <button className={`${styles.contextMenuItem} ${styles.danger}`} onClick={handleDelete} role="menuitem">
            <Trash size={14} aria-hidden="true" /> Удалить
          </button>
        </div>
      )}
    </>
  );
};
