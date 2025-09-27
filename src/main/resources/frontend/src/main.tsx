import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App.tsx';
import { ChatSidebar } from './ChatSidebar.tsx';
import { WelcomePage } from './WelcomePage.tsx';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * Корневой компонент, который определяет основную структуру страницы.
 */
const Root = () => {
    const params = new URLSearchParams(window.location.search);
    const currentSessionId = params.get('sessionId');

    return (
        <div id="root-layout">
            <QueryClientProvider client={queryClient}>
                <Toaster position="bottom-center" />
                <ChatSidebar currentSessionId={currentSessionId} />
                <main>
                    {currentSessionId ? <App sessionId={currentSessionId} /> : <WelcomePage />}
                </main>
            </QueryClientProvider>
        </div>
    );
};

const rootEl = document.getElementById('app-root');
if (rootEl) {
    const root = ReactDOM.createRoot(rootEl);
    root.render(
        <React.StrictMode>
            <Root />
        </React.StrictMode>
    );
} else {
    console.error('Не найден корневой элемент #app-root.');
}
