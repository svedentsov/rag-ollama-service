import React from 'react';
import styles from './StatusIndicator.module.css';

interface StatusIndicatorProps {
    statusText: string;
    elapsedTime: number;
}

/**
 * Компонент для отображения информативного статуса загрузки.
 * Показывает анимированный спиннер, текстовое описание текущего этапа
 * и секундомер, отсчитывающий время выполнения.
 *
 * @param statusText Текст для отображения (например, "Ищу информацию...").
 * @param elapsedTime Количество секунд, прошедших с начала операции.
 */
export const StatusIndicator: React.FC<StatusIndicatorProps> = ({ statusText, elapsedTime }) => {
    return (
        <div className={styles.statusWrapper}>
            <div className={styles.spinner}></div>
            <div className={styles.statusText}>
                {statusText}
                <span className={styles.timer}>{elapsedTime}s</span>
            </div>
        </div>
    );
};
