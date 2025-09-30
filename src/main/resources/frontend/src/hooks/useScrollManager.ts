import { useState, useRef, useEffect, useCallback } from 'react';

const SCROLL_THRESHOLD = 100;

/**
 * @param dependencies Массив зависимостей, при изменении которых нужно проверять скролл.
 * @returns Состояние и функции для управления скроллом в контейнере.
 */
export function useScrollManager(dependencies: React.DependencyList) {
    const containerRef = useRef<HTMLDivElement>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [isAtBottom, setIsAtBottom] = useState(true);

    const scrollToBottom = useCallback((behavior: ScrollBehavior = 'smooth') => {
        messagesEndRef.current?.scrollIntoView({ behavior });
    }, []);

    useEffect(() => {
        const container = containerRef.current;
        if (!container) return;

        const handleScroll = () => {
            const { scrollHeight, scrollTop, clientHeight } = container;
            const atBottom = scrollHeight - scrollTop - clientHeight < SCROLL_THRESHOLD;
            setIsAtBottom(atBottom);
        };

        container.addEventListener('scroll', handleScroll, { passive: true });
        handleScroll();

        return () => container.removeEventListener('scroll', handleScroll);
    }, []);

    useEffect(() => {
        if (isAtBottom) {
            scrollToBottom('smooth');
        }
    }, dependencies);

    return { containerRef, messagesEndRef, showScrollButton: !isAtBottom, scrollToBottom };
}
