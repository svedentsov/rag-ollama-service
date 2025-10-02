import React from 'react';
import styles from './NotificationDot.module.css';

/**
 * Простой UI-компонент, отображающий анимированную точку-уведомление.
 */
export const NotificationDot: React.FC = () => {
  return <span className={styles.notificationDot} aria-label="Новое сообщение"></span>;
};
