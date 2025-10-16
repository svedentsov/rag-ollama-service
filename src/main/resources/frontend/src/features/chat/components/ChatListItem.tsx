import React, { useRef, useState, FC, useEffect } from 'react';
import { ChatSession } from '../../../types';
import { useContextMenu } from '../../../hooks/useContextMenu';
import { NotificationDot } from '../../../components/NotificationDot';
import { Trash, Edit, MoreHorizontal } from 'lucide-react';
import styles from '../ChatSidebar.module.css';

interface ChatListItemProps {
  session: ChatSession;
  isActive: boolean;
  hasNotification: boolean;
  onNavigate: (sessionId: string) => void;
  onRename: (newName: string) => void;
  onDelete: () => void;
}

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
  const { menuState, openMenu, closeMenu } = useContextMenu<ChatSession>(menuRef);

  useEffect(() => {
    if (isEditing && editInputRef.current) {
      editInputRef.current.focus();
      editInputRef.current.select();
    }
  }, [isEditing]);

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    openMenu(e, session);
  };

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
      <div
        onClick={(e) => { e.preventDefault(); onNavigate(session.sessionId); }}
        onContextMenu={handleContextMenu}
        className={`${styles.navLink} ${isActive && !isEditing ? styles.active : ''}`}
        role="button"
        tabIndex={0}
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
            <div className={styles.chatItemActions}>
              {hasNotification && <NotificationDot />}
              <button
                className={styles.moreButton}
                onClick={(e) => {
                  e.stopPropagation();
                  openMenu(e, session);
                }}
                aria-label={`Действия для чата ${session.chatName}`}
              >
                <MoreHorizontal size={16} />
              </button>
            </div>
          </>
        )}
      </div>

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
