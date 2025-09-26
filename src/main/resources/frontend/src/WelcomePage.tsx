import React from 'react';

export function WelcomePage() {
    return (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#555', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
            <h2>Добро пожаловать в платформу AI-агентов!</h2>
            <p>Нажмите "+ Новый чат" в панели слева, чтобы начать работу.</p>
        </div>
    );
}
