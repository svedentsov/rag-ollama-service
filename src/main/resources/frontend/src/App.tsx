import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { v4 as uuidv4 } from 'uuid';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
import { Message, UniversalStreamResponse } from './types';
import { fetchMessages } from './api';
import { Copy, ChevronsUpDown } from 'lucide-react';
import styles from './App.module.css';

interface MessageContextMenuState { show: boolean; x: number; y: number; message: Message | null; }
interface AppProps { sessionId: string; }

const App: React.FC<AppProps> = ({ sessionId }) => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [contextMenu, setContextMenu] = useState<MessageContextMenuState>({ show: false, x: 0, y: 0, message: null });
    const lastRightClickedMessageId = useRef<string | null>(null);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const abortControllerRef = useRef<AbortController | null>(null);
    const lastRequestId = useRef<string | null>(null);
    const queryClient = useQueryClient();

    const { data: history, isLoading: isLoadingHistory, error: historyError } = useQuery({
        queryKey: ['messages', sessionId],
        queryFn: () => fetchMessages(sessionId),
        enabled: !!sessionId,
    });

    useEffect(() => { if (history) setMessages(history); }, [history]);

    useEffect(() => {
        const handleClick = () => {
            setContextMenu(prev => ({ ...prev, show: false }));
            lastRightClickedMessageId.current = null; // Сбрасываем при любом клике
        };
        document.addEventListener('click', handleClick);
        return () => document.removeEventListener('click', handleClick);
    }, []);

    const [isThinking, setIsThinking] = useState(false);
    const [isStreaming, setIsStreaming] = useState(false);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isStreaming, isThinking]);

    const handleStopGenerating = () => {
        if (abortControllerRef.current) abortControllerRef.current.abort();
    };

    const handleSendMessage = async (inputText: string) => {
        if (!inputText.trim() || isStreaming || isThinking) return;

        const userMessage: Message = { id: uuidv4(), type: 'user', text: inputText };
        setMessages(prev => [...prev, userMessage]);
        setIsThinking(true);
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
            let isFirstChunk = true;
            const assistantMessageId = uuidv4();

            while (true) {
                const { value, done } = await reader.read();
                if (done) break;
                buffer += value;
                const lines = buffer.split('\n\n');
                buffer = lines.pop() || '';
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        if (isFirstChunk) {
                            setIsThinking(false); setIsStreaming(true);
                            setMessages(prev => [...prev, { id: assistantMessageId, type: 'assistant', text: '', sources: [] }]);
                            isFirstChunk = false;
                        }
                        const jsonData = line.substring(5).trim();
                        if (!jsonData) continue;
                        try {
                            const eventData = JSON.parse(jsonData) as UniversalStreamResponse;
                            setMessages(currentMessages => {
                                const newMessages = [...currentMessages];
                                let lastMessage = newMessages[newMessages.length - 1];
                                if (!lastMessage || lastMessage.type !== 'assistant') return currentMessages;
                                if (eventData.type === 'task_started') lastRequestId.current = eventData.taskId;
                                else if (eventData.type === 'content') lastMessage.text += eventData.text;
                                else if (eventData.type === 'sources') lastMessage.sources = [...(lastMessage.sources || []), ...eventData.sources];
                                else if (eventData.type === 'error') lastMessage.error = eventData.message;
                                return newMessages;
                            });
                        } catch (e) { console.error('Failed to parse SSE data:', jsonData, e); }
                    }
                }
            }
        } catch (error: any) {
            if (error.name === 'AbortError') console.log('Stream stopped by user.');
            else setMessages(prev => [...prev, { id: uuidv4(), type: 'assistant', text: '', error: error.message || "Произошла неизвестная ошибка." }]);
        } finally {
            setIsThinking(false); setIsStreaming(false); abortControllerRef.current = null;
            queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
        }
    };

    const handleShowContextMenu = (e: React.MouseEvent, message: Message) => {
        if (lastRightClickedMessageId.current === message.id) {
            lastRightClickedMessageId.current = null; // Сбрасываем для следующего раза
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

    return (
        <>
            <div className={styles.chatContainer}>
                <div className={styles.chatMessageContainer}>
                    {isLoadingHistory && <div className={styles.centered}><div className={styles.spinner}></div></div>}
                    {historyError && <div className={styles.errorAlert}>Не удалось загрузить историю сообщений.</div>}
                    {!isLoadingHistory && messages.map((msg) => (
                        <ChatMessage
                            key={msg.id}
                            message={msg}
                            onContextMenu={(e) => handleShowContextMenu(e, msg)}
                        />
                    ))}
                    {isThinking && (
                        <ChatMessage
                            message={{id: 'thinking', type: 'assistant', text: '...'}}
                            isThinking={true}
                            onContextMenu={()=>{}}
                        />
                    )}
                    <div ref={messagesEndRef} />
                </div>
                <ChatInput onSendMessage={handleSendMessage} onStopGenerating={handleStopGenerating} isLoading={isStreaming || isThinking} />
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
