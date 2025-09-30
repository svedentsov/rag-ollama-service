import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App.tsx';
import { ChatSidebar } from './ChatSidebar.tsx';
import { WelcomePage } from './WelcomePage.tsx';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { RouterProvider, useRouter } from './hooks/useRouter.tsx';

/**
 * Клиент TanStack Query для управления состоянием сервера.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 минут
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * Корневой компонент приложения, который управляет основной маршрутизацией
 * и отображением либо страницы приветствия, либо интерфейса чата.
 */
const RootContent = () => {
    const { sessionId } = useRouter();

    return (
        <div id="root-layout">
            <ChatSidebar currentSessionId={sessionId} />
            <main>
                {sessionId ? <App sessionId={sessionId} /> : <WelcomePage />}
            </main>
        </div>
    );
};


/**
 * Точка входа приложения.
 * Оборачивает все приложение в необходимые провайдеры.
 */
const Root = () => {
    return (
        <QueryClientProvider client={queryClient}>
            <Toaster position="bottom-center" />
            <RouterProvider>
                <RootContent />
            </RouterProvider>
        </QueryClientProvider>
    );
};

const rootEl = document.getElementById('app-root');
if (rootEl) {
    ReactDOM.createRoot(rootEl).render(
        <React.StrictMode>
            <Root />
        </React.StrictMode>
    );
} else {
    console.error('Не найден корневой элемент #app-root.');
}
