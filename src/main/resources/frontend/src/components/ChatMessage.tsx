import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { darcula } from 'react-syntax-highlighter/dist/esm/styles/prism';
import toast from 'react-hot-toast';
import { Message } from '../types';
import { Copy } from 'lucide-react';
import styles from './ChatMessage.module.css';

/**
 * Пропсы для компонента ChatMessage.
 * @param message - Объект сообщения для отображения.
 * @param onContextMenu - Функция-обработчик для вызова контекстного меню.
 * @param isThinking - Флаг для отображения состояния "размышления".
 */
interface ChatMessageProps {
    message: Message;
    onContextMenu: (event: React.MouseEvent) => void;
    isThinking?: boolean;
}

/**
 * Компонент-обертка для блока кода с кнопкой копирования.
 */
const CodeBlock: React.FC<React.PropsWithChildren<{ className?: string }>> = ({ className, children }) => {
    const [isCopied, setIsCopied] = useState(false);
    const textToCopy = String(children).replace(/\n$/, '');

    const handleCopy = () => {
        navigator.clipboard.writeText(textToCopy).then(() => {
            setIsCopied(true);
            toast.success('Скопировано в буфер обмена!');
            setTimeout(() => setIsCopied(false), 2000);
        }, () => {
            toast.error('Не удалось скопировать.');
        });
    };

    const language = className?.replace('language-', '') || 'text';

    return (
        <div className={styles.codeBlockWrapper}>
            <div className={styles.codeHeader}>
                <span>{language}</span>
                <button className={styles.btnCopy} onClick={handleCopy} aria-label="Скопировать код">
                    <Copy size={14}/> {isCopied ? 'Скопировано!' : 'Копировать'}
                </button>
            </div>
            <SyntaxHighlighter
                style={darcula}
                language={language}
                PreTag="div"
                customStyle={{ margin: 0, padding: '1rem', borderRadius: '0 0 var(--radius) var(--radius)' }}
            >
                {textToCopy}
            </SyntaxHighlighter>
        </div>
    );
};

/**
 * Компонент для отображения одного сообщения в чате в минималистичном стиле.
 * @param props - Пропсы компонента.
 */
export function ChatMessage({ message, onContextMenu, isThinking = false }: ChatMessageProps) {
    const isUser = message.type === 'user';

    if (isThinking) {
        return (
            <div className={`${styles.messageWrapper} ${styles.assistant}`}>
                <div className={styles.thinkingBubble}>
                    <div className={styles.dotFlashing}></div>
                </div>
            </div>
        )
    }

    return (
        <div onContextMenu={onContextMenu} className={`${styles.messageWrapper} ${isUser ? styles.user : styles.assistant}`}>
            <div className={`${styles.bubble} ${isUser ? styles.userBubble : styles.assistantBubble} ${message.error ? styles.isError : ''}`}>
                <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    components={{
                        code: ({ node, className, children, ...props }) => {
                            const match = /language-(\w+)/.exec(className || '');
                            return match ? (
                                <CodeBlock className={className}>{children}</CodeBlock>
                            ) : (
                                <code className={styles.inlineCode} {...props}>{children}</code>
                            );
                        },
                        table: ({node, ...props}) => <table className={styles.markdownTable} {...props} />,
                        p: ({node, ...props}) => <p className={styles.paragraph} {...props} />
                    }}
                >
                    {message.text || (message.error ? `**Ошибка:** ${message.error}` : '')}
                </ReactMarkdown>
            </div>
        </div>
    );
}
