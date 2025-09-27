import React from 'react';
import styles from './WelcomePage.module.css';

export function WelcomePage() {
    return (
        <div className={styles.welcomeContainer}>
            <h2>Добро пожаловать в платформу AI-агентов!</h2>
            <p>Нажмите "+ Новый чат" в панели слева, чтобы начать работу.</p>
        </div>
    );
}
