import React from 'react';
import ReactDOM from 'react-dom/client';
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';

import { ChatPage } from './ChatPage.tsx';
import { ChatSidebar } from './ChatSidebar.tsx';
import { WelcomePage } from './WelcomePage.tsx';

// --- Монтирование Сайдбара ---
const sidebarRootEl = document.getElementById('sidebar-root');
if (sidebarRootEl) {
    const params = new URLSearchParams(window.location.search);
    const currentSessionId = params.get('sessionId');
    const sidebarRoot = ReactDOM.createRoot(sidebarRootEl);
    sidebarRoot.render(
        <React.StrictMode>
            <ChatSidebar currentSessionId={currentSessionId} />
        </React.StrictMode>
    );
} else {
    console.error('Не найден корневой элемент #sidebar-root для сайдбара.');
}


// --- Монтирование Основного Контента ---
const chatWidgetRootEl = document.getElementById('chat-widget-root');
if (chatWidgetRootEl) {
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get('sessionId');
    const chatWidgetRoot = ReactDOM.createRoot(chatWidgetRootEl);

    chatWidgetRoot.render(
        <React.StrictMode>
            {sessionId ? <ChatPage sessionId={sessionId} /> : <WelcomePage />}
        </React.StrictMode>
    );
} else {
    console.error('Не найден корневой элемент #chat-widget-root для основного контента.');
}