import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';

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
 * @param {ReactNode} props.children - Дочерние компоненты.
 */
export const RouterProvider = ({ children }: { children: ReactNode }) => {
  const [sessionId, setSessionId] = useState<string | null>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('sessionId');
  });

  // Обрабатывает кнопки "вперед/назад" в браузере
  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search);
      setSessionId(params.get('sessionId'));
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  /**
   * Функция для программной навигации без перезагрузки страницы.
   */
  const navigate = useCallback((newSessionId: string | null) => {
    const newUrl = newSessionId ? `/chat?sessionId=${newSessionId}` : '/';
    const currentUrl = window.location.pathname + window.location.search;

    if (newUrl !== currentUrl) {
      window.history.pushState({ sessionId: newSessionId }, '', newUrl);
      setSessionId(newSessionId);
    }
  }, []);

  return (
    <RouterContext.Provider value={{ sessionId, navigate }}>
      {children}
    </RouterContext.Provider>
  );
};

/**
 * Хук для доступа к состоянию и функциям роутера.
 * @returns {RouterContextType} Объект с текущим sessionId и функцией navigate.
 */
export const useRouter = (): RouterContextType => {
  const context = useContext(RouterContext);
  if (!context) {
    throw new Error('useRouter должен использоваться внутри RouterProvider');
  }
  return context;
};
