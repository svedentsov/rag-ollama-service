import React, { useState, useEffect, useRef } from 'react';
import { Spinner, Button, Form } from 'react-bootstrap';
import { ChatMessage } from './ChatMessage';
import { ChatInput } from './ChatInput';
import { Message, UniversalStreamResponse } from './types';
import { fetchMessages, updateChatName, fetchChatSessions } from './api';
import BotIcon from './assets/bot.svg?react';
import EditIcon from './assets/edit.svg?react';

interface ChatPageProps {
    sessionId: string;
}

export function ChatPage({ sessionId }: ChatPageProps) {
    const [messages, setMessages] = useState<Message[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [chatName, setChatName] = useState('Загрузка...');
    const [originalChatName, setOriginalChatName] = useState('');
    const [isEditingName, setIsEditingName] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const nameInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        const loadChatData = async () => {
            if (!sessionId) {
                setMessages([{ type: 'assistant', text: "Создайте новый чат, чтобы начать." }]);
                setChatName("Новый чат");
                setIsLoading(false);
                return;
            }
            try {
                setIsLoading(true);
                const sessions = await fetchChatSessions();
                const currentSession = sessions.find(s => s.sessionId === sessionId);
                if (currentSession) {
                    setChatName(currentSession.chatName);
                    setOriginalChatName(currentSession.chatName);
                }
                const history = await fetchMessages(sessionId);
                setMessages(history);
                const params = new URLSearchParams(window.location.search);
                if (params.get('action') === 'rename') {
                    setIsEditingName(true);
                }
            } catch (error) {
                console.error('Failed to load chat data:', error);
                setMessages([{ type: 'assistant', text: "Ошибка при загрузке чата." }]);
                setChatName("Ошибка");
            } finally {
                setIsLoading(false);
            }
        };
        loadChatData();
    }, [sessionId]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isLoading]);

    useEffect(() => {
        if (isEditingName) {
            nameInputRef.current?.focus();
            const params = new URLSearchParams(window.location.search);
            if(params.get('action') === 'rename') {
                params.delete('action');
                const newUrl = `${window.location.pathname}?${params.toString()}`;
                window.history.replaceState({}, '', newUrl);
            }
        }
    }, [isEditingName]);

    const handleNameSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsEditingName(false);
        if (chatName.trim() === '' || chatName === originalChatName) {
            setChatName(originalChatName);
            return;
        }
        try {
            await updateChatName(sessionId, chatName);
            setOriginalChatName(chatName);
        } catch (error) {
            console.error("Failed to update chat name", error);
            alert("Не удалось переименовать чат.");
            setChatName(originalChatName);
        }
    };

    const handleSendMessage = async (inputText: string) => {
        if (!inputText.trim() || isLoading) return;

        const userMessage: Message = { type: 'user', text: inputText };
        setMessages(prev => [...prev, userMessage]);
        setIsLoading(true);

        try {
            const response = await fetch('/api/v1/orchestrator/ask-stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: inputText, sessionId: sessionId }),
            });

            if (!response.body) return;

            // Добавляем пустой плейсхолдер для ответа ассистента
            setMessages(prev => [...prev, { type: 'assistant', text: '', sources: [] }]);

            const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
            let buffer = '';

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

                            setMessages(currentMessages => {
                                const newMessages = [...currentMessages];
                                let lastMessage = newMessages[newMessages.length - 1];

                                if (!lastMessage || lastMessage.type !== 'assistant') {
                                    return currentMessages; // Should not happen with our placeholder logic
                                }

                                if (eventData.type === 'content') {
                                    lastMessage.text += eventData.text;
                                } else if (eventData.type === 'sources') {
                                    lastMessage.sources = [...(lastMessage.sources || []), ...eventData.sources];
                                }
                                return newMessages;
                            });
                        } catch (e) {
                            console.error('Failed to parse SSE data:', jsonData, e);
                        }
                    }
                }
            }
        } catch (error) {
            console.error("Error during streaming:", error);
            const errorMessage: Message = { type: 'assistant', text: "Произошла ошибка при получении ответа." };
            setMessages(prev => [...prev.slice(0, -1), errorMessage]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="d-flex flex-column h-100 bg-white">
            <div className="p-3 border-bottom chat-header">
                {isEditingName ? (
                    <Form onSubmit={handleNameSubmit} className="flex-grow-1">
                        <Form.Control
                            ref={nameInputRef}
                            value={chatName}
                            onChange={e => setChatName(e.target.value)}
                            onBlur={e => handleNameSubmit(e as any)}
                            size="sm"
                        />
                    </Form>
                ) : (
                    <>
                        <h5 className="mb-0">{chatName}</h5>
                        <Button variant="link" className="p-0 btn-edit" onClick={() => setIsEditingName(true)}>
                            <EditIcon />
                        </Button>
                    </>
                )}
            </div>
            <div className="flex-grow-1 overflow-auto p-3 chat-message-container">
                {messages.map((msg, index) => (
                    <ChatMessage key={index} message={msg} />
                ))}
                 {isLoading && (!messages.length || messages[messages.length - 1].type === 'user') && (
                     <div className="d-flex justify-content-start mb-4">
                        <div className="d-flex gap-3 mw-75">
                            <div className="avatar mt-1"><BotIcon /></div>
                            <div className="message-bubble assistant">
                                 <Spinner animation="grow" size="sm" />
                            </div>
                        </div>
                    </div>
                )}
                <div ref={messagesEndRef} />
            </div>
            <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} />
        </div>
    );
}