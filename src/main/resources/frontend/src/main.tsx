import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

// Pages
import { WelcomePage } from './pages/WelcomePage';

// Features
import { App as ChatApp } from './features/chat/App';
import { ChatSidebar } from './features/chat/ChatSidebar';
import { FileManager } from './features/file-manager/FileManager';

// Shared UI Components
import { RootSidebar } from './components/RootSidebar';

// App Logic
import { RouterProvider, useRouter } from './hooks/useRouter';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      refetchOnWindowFocus: false,
    },
  },
});

const MainContent = () => {
    const { pathname, sessionId } = useRouter();

    if (pathname.startsWith('/files')) {
        return <FileManager />;
    }

    if (pathname.startsWith('/chat') || pathname === '/') {
        return (
            <>
                <ChatSidebar currentSessionId={sessionId} />
                <main>
                    {sessionId ? <ChatApp sessionId={sessionId} /> : <WelcomePage />}
                </main>
            </>
        );
    }

    return <WelcomePage />;
};

const RootContent = () => {
    return (
        <div id="root-layout">
            <RootSidebar />
            <MainContent />
        </div>
    );
};

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
