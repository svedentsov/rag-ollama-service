import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { useSessionStore } from '../state/sessionStore';

/**
 * @interface RouterContextType
 * @description Определяет контракт для контекста роутера.
 */
interface RouterContextType {
  /** @property {string | null} sessionId - ID текущей активной сессии из URL. */
  sessionId: string | null;
  /**
   * @function navigate
   * @description Функция для программной навигации, которая обновляет URL и состояние.
   * @param {string | null} sessionId - ID сессии для навигации или null для перехода на главную.
   */
  navigate: (sessionId: string | null) => void;
}

/**
 * Контекст React для предоставления доступа к состоянию роутера.
 */
const RouterContext = createContext<RouterContextType | null>(null);

/**
 * Провайдер, который инкапсулирует всю логику легковесного роутера.
 * Он синхронизирует состояние React с URL браузера, являясь единым источником правды.
 * @param {{ children: ReactNode }} props - Дочерние компоненты.
 */
export const RouterProvider = ({ children }: { children: ReactNode }) => {
  const setCurrentSessionIdInStore = useSessionStore((state) => state.setCurrentSessionId);

  /**
   * Инициализирует состояние из URL при первой загрузке.
   */
  const [sessionId, setSessionId] = useState<string | null>(() => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('sessionId');
    setCurrentSessionIdInStore(id); // Первичная синхронизация с глобальным стором
    return id;
  });

  /**
   * Подписывается на события навигации браузера (кнопки "вперед/назад").
   */
  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search);
      const id = params.get('sessionId');
      setSessionId(id);
      setCurrentSessionIdInStore(id);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [setCurrentSessionIdInStore]);

  /**
   * Функция для программной навигации.
   */
  const navigate = useCallback((newSessionId: string | null) => {
    const newUrl = newSessionId ? `/chat?sessionId=${newSessionId}` : '/';
    const currentUrl = window.location.pathname + window.location.search;

    if (newUrl !== currentUrl) {
      window.history.pushState({ sessionId: newSessionId }, '', newUrl);
      setSessionId(newSessionId);
      setCurrentSessionIdInStore(newSessionId);
    }
  }, [setCurrentSessionIdInStore]);

  const value = { sessionId, navigate };

  return (
    <RouterContext.Provider value={value}>
      {children}
    </RouterContext.Provider>
  );
};

/**
 * Хук для удобного доступа к состоянию и функциям роутера из любого компонента.
 * @returns {RouterContextType} Объект с `sessionId` и функцией `navigate`.
 */
export const useRouter = (): RouterContextType => {
  const context = useContext(RouterContext);
  if (!context) {
    throw new Error('Хук useRouter должен использоваться внутри компонента RouterProvider');
  }
  return context;
};
