import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { useSessionStore } from '../state/sessionStore';

/**
 * @interface RouterContextType
 * @description Определяет контракт для контекста роутера.
 */
interface RouterContextType {
  /** @property {string} pathname - Текущий путь в URL (например, "/chat" или "/files"). */
  pathname: string;
  /** @property {string | null} sessionId - ID текущей активной сессии из URL. */
  sessionId: string | null;
  /**
   * @function navigate
   * @description Функция для программной навигации, которая обновляет URL и состояние.
   * @param {string | null} path - Целевой путь. Если `null`, переходит на главную.
   */
  navigate: (path: string | null) => void;
}

const RouterContext = createContext<RouterContextType | null>(null);

/**
 * @description Хук для получения текущего состояния URL.
 * @returns {{ pathname: string, sessionId: string | null }}
 */
const useLocationState = () => {
    const [state, setState] = useState(() => {
        const params = new URLSearchParams(window.location.search);
        return {
            pathname: window.location.pathname,
            sessionId: params.get('sessionId')
        };
    });

    useEffect(() => {
        const handlePopState = () => {
            const params = new URLSearchParams(window.location.search);
            setState({
                pathname: window.location.pathname,
                sessionId: params.get('sessionId')
            });
        };
        window.addEventListener('popstate', handlePopState);
        return () => window.removeEventListener('popstate', handlePopState);
    }, []);

    const setLocation = useCallback((pathname: string, sessionId: string | null) => {
        const newUrl = sessionId ? `${pathname}?sessionId=${sessionId}` : pathname;
        const currentUrl = window.location.pathname + window.location.search;
        if (newUrl !== currentUrl) {
            window.history.pushState({ pathname, sessionId }, '', newUrl);
            setState({ pathname, sessionId });
        }
    }, []);

    return { ...state, setLocation };
};

/**
 * Провайдер, который инкапсулирует всю логику легковесного роутера.
 * @param {{ children: ReactNode }} props - Дочерние компоненты.
 */
export const RouterProvider = ({ children }: { children: ReactNode }) => {
  const setCurrentSessionIdInStore = useSessionStore((state) => state.setCurrentSessionId);
  const { pathname, sessionId, setLocation } = useLocationState();

  useEffect(() => {
    setCurrentSessionIdInStore(sessionId);
  }, [sessionId, setCurrentSessionIdInStore]);

  const navigate = useCallback((path: string | null) => {
    if (path === null) {
        setLocation('/', null);
        return;
    }
    const url = new URL(path, window.location.origin);
    const newSessionId = url.searchParams.get('sessionId');
    setLocation(url.pathname, newSessionId);
  }, [setLocation]);

  const value = { pathname, sessionId, navigate };

  return (
    <RouterContext.Provider value={value}>
      {children}
    </RouterContext.Provider>
  );
};

/**
 * Хук для удобного доступа к состоянию и функциям роутера из любого компонента.
 * @returns {RouterContextType} Объект с `pathname`, `sessionId` и функцией `navigate`.
 */
export const useRouter = (): RouterContextType => {
  const context = useContext(RouterContext);
  if (!context) {
    throw new Error('Хук useRouter должен использоваться внутри компонента RouterProvider');
  }
  return context;
};
