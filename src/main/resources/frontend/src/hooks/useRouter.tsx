import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { useSessionStore } from '../state/sessionStore';

/**
 * Контекст для роутера.
 */
interface RouterContextType {
  sessionId: string | null;
  navigate: (sessionId: string | null) => void;
}

const RouterContext = createContext<RouterContextType | null>(null);

/**
 * Провайдер для легковесного роутера.
 * @param {object} props - Пропсы компонента.
 */
export const RouterProvider = ({ children }: { children: ReactNode }) => {
  const setCurrentSessionId = useSessionStore((state) => state.setCurrentSessionId);

  const [sessionId, setSessionId] = useState<string | null>(() => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('sessionId');
    setCurrentSessionId(id); // Первичная синхронизация
    return id;
  });

  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search);
      const id = params.get('sessionId');
      setSessionId(id);
      setCurrentSessionId(id); // Синхронизация при навигации "вперед/назад"
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [setCurrentSessionId]);

  const navigate = useCallback((newSessionId: string | null) => {
    const newUrl = newSessionId ? `/chat?sessionId=${newSessionId}` : '/';
    const currentUrl = window.location.pathname + window.location.search;

    if (newUrl !== currentUrl) {
      window.history.pushState({ sessionId: newSessionId }, '', newUrl);
      setSessionId(newSessionId);
      setCurrentSessionId(newSessionId); // Синхронизация при программной навигации
    }
  }, [setCurrentSessionId]);

  return (
    <RouterContext.Provider value={{ sessionId, navigate }}>
      {children}
    </RouterContext.Provider>
  );
};

/**
 * Хук для доступа к состоянию и функциям роутера.
 */
export const useRouter = (): RouterContextType => {
  const context = useContext(RouterContext);
  if (!context) {
    throw new Error('useRouter должен использоваться внутри RouterProvider');
  }
  return context;
};
