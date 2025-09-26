import React, { useState, useRef, useEffect } from 'react';
import { Form, Button, InputGroup } from 'react-bootstrap';
import SendIcon from './assets/send.svg?react';

interface ChatInputProps {
    onSendMessage: (text: string) => void;
    isLoading: boolean;
}

export function ChatInput({ onSendMessage, isLoading }: ChatInputProps) {
    const [inputText, setInputText] = useState('');
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
        }
    }, [inputText]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (inputText.trim() && !isLoading) {
            onSendMessage(inputText);
            setInputText('');
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit(e as unknown as React.FormEvent);
        }
    };

    return (
        <div className="p-3 chat-input-container">
            <Form onSubmit={handleSubmit} className="chat-input-form">
                <InputGroup>
                    <Form.Control
                        ref={textareaRef}
                        as="textarea"
                        rows={1}
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="Спросите что-нибудь..."
                        disabled={isLoading}
                        className="py-2 px-3"
                        style={{ resize: 'none', maxHeight: '150px' }}
                    />
                    <Button
                        type="submit"
                        variant="dark"
                        disabled={isLoading || !inputText.trim()}
                        className="btn-send"
                    >
                        <SendIcon />
                    </Button>
                </InputGroup>
            </Form>
        </div>
    );
}