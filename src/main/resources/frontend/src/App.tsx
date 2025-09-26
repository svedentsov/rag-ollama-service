// C:\Users\svede\IdeaProjects\rag-ollama-service\src/main/resources/frontend/src/App.tsx
import React, { useState, useRef, useEffect } from 'react';
import { Card, Spinner } from 'react-bootstrap';
import { ChatMessage } from './components/ChatMessage';
import { ChatInput } from './components/ChatInput';
// ИСПРАВЛЕНО: Импортируем тип Message из централизованного файла
import { Message } from './types';

/**
 * @typedef {object} AppProps
 * @property {string | null} sessionId - ID текущей сессии чата, полученный от сервера.
 */
type AppProps = {
  sessionId: string | null;
};

/**
 * Корневой компонент виджета чата.
 * Управляет состоянием сообщений, загрузки и взаимодействием с API.
 * @param {AppProps} props - Пропсы компонента.
 */
const App: React.FC<AppProps> = ({ sessionId }) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(sessionId);
  const messageEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const loadHistory = async () => {
      setIsLoading(true);
      if (currentSessionId) {
        setMessages([
          { type: 'assistant', text: `История для чата **${currentSessionId}**.` }
        ]);
      } else {
        setMessages([{ type: 'assistant', text: 'Здравствуйте! Чем я могу помочь?' }]);
      }
      setIsLoading(false);
    };
    loadHistory();
  }, [currentSessionId]);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  const handleSendMessage = async (text: string) => {
    setMessages(prev => [...prev, { type: 'user', text }]);
    setIsLoading(true);

    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      const mockResponse: Message = {
        type: 'assistant',
        text: `Это **симуляция** ответа на ваше сообщение:\n\n\`\`\`java\nSystem.out.println("${text}");\n\`\`\``,
        sources: [{ sourceName: 'документ1.pdf', textSnippet: 'Фрагмент текста из документа 1.' }]
      };
      setMessages(prev => [...prev, mockResponse]);

    } catch (error) {
      const errorMessage: Message = {
        type: 'assistant',
        text: 'К сожалению, произошла ошибка при отправке сообщения.'
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="h-100 d-flex flex-column shadow-sm">
      <Card.Header as="h5">AI Ассистент</Card.Header>
      <Card.Body className="flex-grow-1 overflow-auto p-3">
        {messages.map((msg, index) => (
          <ChatMessage key={index} message={msg} />
        ))}
        {isLoading && (
          <div className="d-flex align-items-center text-muted">
            <Spinner animation="grow" size="sm" className="me-2" />
            <span>Печатает...</span>
          </div>
        )}
        <div ref={messageEndRef} />
      </Card.Body>
      <ChatInput onSendMessage={handleSendMessage} isLoading={isLoading} />
    </Card>
  );
};

export default App;
