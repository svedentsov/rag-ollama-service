import React from 'react';
import styles from './WelcomePage.module.css';

/**
 * Страница-заглушка, которая отображается, когда ни один чат не выбран.
 * @returns {React.ReactElement}
 */
export function WelcomePage() {
    return (
        <div className={styles.welcomeContainer}>
            <h2>Добро пожаловать в платформу AI-агентов!</h2>
            <p>Выберите чат или нажмите "+ Новый чат" в панели слева, чтобы начать работу.</p>
        </div>
    );
}
