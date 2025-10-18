import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

// Pages
import { WelcomePage } from './pages/WelcomePage';
import { FileManagerPage } from './features/file-manager/FileManagerPage';

// Features
import { App as ChatApp } from './features/chat/App';
import { ChatSidebar } from './features/chat/ChatSidebar';

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
        return <FileManagerPage />;
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

const RootLayout = () => {
    return (
        <div id="root-layout">
            <RootSidebar />
            <MainContent />
        </div>
    );
};

const AppRoot = () => {
    return (
        <QueryClientProvider client={queryClient}>
            <Toaster position="bottom-center" />
            <RouterProvider>
                <RootLayout />
            </RouterProvider>
        </QueryClientProvider>
    );
};

const rootEl = document.getElementById('app-root');
if (rootEl) {
    ReactDOM.createRoot(rootEl).render(
        <React.StrictMode>
            <AppRoot />
        </React.StrictMode>
    );
} else {
    console.error('Не найден корневой элемент #app-root.');
}
