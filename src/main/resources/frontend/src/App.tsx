import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message, UniversalStreamResponse } from './types';
import { fetchMessages } from './api';
import { Copy } from 'lucide-react';
import { ScrollToBottomButton } from "./components/ScrollToBottomButton";
import { StatusIndicator } from "./components/StatusIndicator";
import styles from './App.module.css';

interface MessageContextMenuState { show: boolean; x: number; y: number; message: Message | null; }
interface AppProps { sessionId: string; }

// Порог в пикселях. Если пользователь находится ближе к низу, считаем, что он внизу.
const SCROLL_THRESHOLD = 100;

const App: React.FC<AppProps> = ({ sessionId }) => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [contextMenu, setContextMenu] = useState<MessageContextMenuState>({ show: false, x: 0, y: 0, message: null });
    const lastRightClickedMessageId = useRef<string | null>(null);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const abortControllerRef = useRef<AbortController | null>(null);
    const lastRequestId = useRef<string | null>(null);
    const queryClient = useQueryClient();

    // Refs и State для интеллектуального скролла и плавной анимации
    const chatContainerRef = useRef<HTMLDivElement>(null);
    const [isAtBottom, setIsAtBottom] = useState(true);
    const textBufferRef = useRef<string>('');
    const animationFrameRef = useRef<number | null>(null);

    // Состояния для управления UI во время ожидания
    const [isThinking, setIsThinking] = useState(false);
    const [statusText, setStatusText] = useState<string | null>(null);
    const [elapsedTime, setElapsedTime] = useState(0);
    const [isStreaming, setIsStreaming] = useState(false);

    const { data: history, isLoading: isLoadingHistory, error: historyError } = useQuery({
        queryKey: ['messages', sessionId],
        queryFn: () => fetchMessages(sessionId),
        enabled: !!sessionId,
    });

    useEffect(() => { if (history) setMessages(history); }, [history]);

    useEffect(() => {
        const handleClick = () => {
            const selection = window.getSelection();
            if (selection && !selection.isCollapsed) {
                return;
            }
            setContextMenu(prev => ({ ...prev, show: false }));
            lastRightClickedMessageId.current = null;
        };
        document.addEventListener('click', handleClick);
        return () => document.removeEventListener('click', handleClick);
    }, []);

    // Условный автоскролл
    useEffect(() => {
        if (isAtBottom) {
            messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages, isStreaming, isThinking, isAtBottom]);

    // Обработчик события скролла
    useEffect(() => {
        const container = chatContainerRef.current;
        if (!container) return;

        const handleScroll = () => {
            const { scrollHeight, scrollTop, clientHeight } = container;
            const atBottom = scrollHeight - scrollTop - clientHeight < SCROLL_THRESHOLD;
            setIsAtBottom(atBottom);
        };

        container.addEventListener('scroll', handleScroll);
        handleScroll(); // Устанавливаем начальное состояние

        return () => container.removeEventListener('scroll', handleScroll);
    }, [chatContainerRef]);

    // Секундомер
    useEffect(() => {
        let timerInterval: number | undefined;
        if (isThinking) {
            timerInterval = window.setInterval(() => {
                setElapsedTime(prev => prev + 1);
            }, 1000);
        } else {
            clearInterval(timerInterval);
            setElapsedTime(0);
        }
        return () => clearInterval(timerInterval);
    }, [isThinking]);

    // Анимация печати
    useEffect(() => {
        const typeCharacter = () => {
            if (textBufferRef.current.length > 0) {
                const bufferSize = textBufferRef.current.length;
                const charsToType = Math.max(1, Math.min(Math.floor(bufferSize * 0.1), 10));

                const chunk = textBufferRef.current.substring(0, charsToType);
                textBufferRef.current = textBufferRef.current.substring(charsToType);

                setMessages(prev => {
                    if (prev.length === 0 || prev[prev.length - 1].type !== 'assistant') {
                        return prev;
                    }
                    const newMessages = [...prev];
                    const lastMessage = newMessages[newMessages.length - 1];
                    lastMessage.text += chunk;
                    return newMessages;
                });
            }

            if (isStreaming || textBufferRef.current.length > 0) {
                const interval = textBufferRef.current.length > 50 ? 10 : 30;
                setTimeout(() => {
                    animationFrameRef.current = requestAnimationFrame(typeCharacter);
                }, interval);
            }
        };

        if (isStreaming) {
            animationFrameRef.current = requestAnimationFrame(typeCharacter);
        } else {
             if (textBufferRef.current.length > 0) {
                 setMessages(prev => {
                    if (prev.length === 0 || prev[prev.length - 1].type !== 'assistant') return prev;
                    const newMessages = [...prev];
                    const lastMessage = newMessages[newMessages.length - 1];
                    lastMessage.text += textBufferRef.current;
                    textBufferRef.current = '';
                    return newMessages;
                });
            }
        }

        return () => {
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
            }
        };
    }, [isStreaming]);

    const handleStopGenerating = () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
    };

    const handleSendMessage = async (inputText: string) => {
        if (!inputText.trim() || isStreaming || isThinking) return;

        scrollToBottom();
        const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
        setMessages(prev => [...prev, userMessage]);

        setIsThinking(true);
        setStatusText("Подготовка ответа...");
        textBufferRef.current = '';
        abortControllerRef.current = new AbortController();

        try {
            const response = await fetch('/api/v1/orchestrator/ask-stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: inputText, sessionId }),
                signal: abortControllerRef.current.signal,
            });

            if (!response.body) throw new Error("Stream body is missing");
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

            const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
            let buffer = '';
            let isFirstContentChunk = true;
            const assistantMessageId = uuidv4();

            while (true) {
                const { value, done } = await reader.read();
                if (done) break;
                buffer += value;
                const lines = buffer.split('\n\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const jsonData = line.substring(5).trim();
                        if (!jsonData) continue;

                        try {
                            const eventData = JSON.parse(jsonData) as UniversalStreamResponse;

                            if (eventData.type === 'status_update') {
                                setStatusText(eventData.text);
                            } else if (eventData.type === 'content') {
                                if (isFirstContentChunk) {
                                    setIsThinking(false);
                                    setStatusText(null);
                                    setIsStreaming(true);
                                    setMessages(prev => [...prev, { id: assistantMessageId, type: 'assistant', text: '', sources: [] }]);
                                    isFirstContentChunk = false;
                                }
                                textBufferRef.current += eventData.text;
                            } else {
                                setMessages(currentMessages => {
                                    const newMessages = [...currentMessages];
                                    let lastMessage = newMessages[newMessages.length - 1];
                                    if (!lastMessage || lastMessage.type !== 'assistant') return currentMessages;

                                    if (eventData.type === 'task_started') lastRequestId.current = eventData.taskId;
                                    else if (eventData.type === 'sources') lastMessage.sources = [...(lastMessage.sources || []), ...eventData.sources];
                                    else if (eventData.type === 'error') lastMessage.error = eventData.message;

                                    return newMessages;
                                });
                            }
                        } catch (e) { console.error('Failed to parse SSE data:', jsonData, e); }
                    }
                }
            }
        } catch (error: any) {
            if (error.name === 'AbortError') {
                console.log('Stream stopped by user.');
            } else {
                setMessages(prev => [...prev, { id: uuidv4(), type: 'assistant', text: '', error: error.message || "Произошла неизвестная ошибка." }]);
            }
        } finally {
            setIsThinking(false);
            setIsStreaming(false);
            setStatusText(null);
            abortControllerRef.current = null;
            queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
        }
    };

    const handleShowContextMenu = (e: React.MouseEvent, message: Message) => {
        if (lastRightClickedMessageId.current === message.id) {
            lastRightClickedMessageId.current = null;
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        setContextMenu({ show: true, x: e.clientX, y: e.clientY, message });
        lastRightClickedMessageId.current = message.id;
    };

    const handleCopySelectedMessage = () => {
        if (contextMenu.message) {
            navigator.clipboard.writeText(contextMenu.message.text)
                .then(() => toast.success('Сообщение скопировано!'))
                .catch(() => toast.error('Не удалось скопировать.'));
        }
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        setIsAtBottom(true);
    };

    return (
        <>
            <div className={styles.chatContainer}>
                <div className={styles.chatMessageContainer} ref={chatContainerRef}>
                    {isLoadingHistory && <div className={styles.centered}><div className={styles.spinner}></div></div>}
                    {historyError && <div className={styles.errorAlert}>Не удалось загрузить историю сообщений.</div>}
                    {!isLoadingHistory && messages.map((msg) => (
                        <ChatMessage
                            key={msg.id}
                            message={msg}
                            onContextMenu={(e) => handleShowContextMenu(e, msg)}
                        />
                    ))}

                    {isThinking && statusText && (
                        <StatusIndicator statusText={statusText} elapsedTime={elapsedTime} />
                    )}

                    <div ref={messagesEndRef} />
                </div>
                <ChatInput
                    onSendMessage={handleSendMessage}
                    onStopGenerating={handleStopGenerating}
                    isLoading={isStreaming || isThinking}
                    showScrollButton={!isAtBottom}
                    onScrollToBottom={scrollToBottom}
                />
            </div>

            {contextMenu.show && contextMenu.message && (
                <div className={styles.messageContextMenu} style={{ top: contextMenu.y, left: contextMenu.x }}>
                    <button className={styles.contextMenuItem} onClick={handleCopySelectedMessage}>
                        <Copy size={14} /> Копировать
                    </button>
                </div>
            )}
        </>
    );
};

export default App;
