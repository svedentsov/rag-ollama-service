import React from 'react';
import { Message } from './types';
import { marked } from 'marked';
import Prism from 'prismjs';
import 'prismjs/themes/prism-dark.css'; // Тема для подсветки кода
import UserIcon from './assets/user.svg?react';
import BotIcon from './assets/bot.svg?react';

// Настраиваем marked для использования Prism.js и добавления классов Bootstrap
const renderer = new marked.Renderer();
renderer.table = (header, body) => {
    return `<div class="table-responsive"><table class="table table-bordered table-striped table-sm">${header}${body}</table></div>`;
};
renderer.heading = (text, level) => {
    return `<h${level}>${text}</h${level}>`;
};
renderer.code = (code, lang) => {
    const language = lang || 'plaintext';
    if (Prism.languages[language]) {
        const highlighted = Prism.highlight(code, Prism.languages[language], language);
        return `<pre class="language-${language}"><code>${highlighted}</code></pre>`;
    }
    return `<pre><code>${code}</code></pre>`;
};

marked.setOptions({
    renderer: renderer,
    gfm: true,
    breaks: true,
});

interface ChatMessageProps {
    message: Message;
}

export function ChatMessage({ message }: ChatMessageProps) {
    const isUser = message.type === 'user';

    // Функция для создания HTML из Markdown
    const createMarkup = () => {
        if (!message.text) return { __html: '' };
        return { __html: marked.parse(message.text) as string };
    };

    const bubbleClasses = ['message-bubble', isUser ? 'user' : 'assistant'];

    return (
        <div className={`d-flex mb-4 ${isUser ? 'justify-content-end' : 'justify-content-start'}`}>
            <div className={`d-flex gap-3 mw-75 ${isUser ? 'flex-row-reverse' : ''}`}>
                <div className="avatar mt-1">
                    {isUser ? <UserIcon /> : <BotIcon />}
                </div>
                <div className="flex-grow-1">
                    <div className={bubbleClasses.join(' ')} dangerouslySetInnerHTML={createMarkup()} />
                    {!isUser && message.sources && message.sources.length > 0 && (
                        <div className="mt-2 small text-muted">
                            <strong>Источники:</strong>
                            <ul className="list-unstyled mb-0 ps-3">
                                {message.sources.map((source, index) => (
                                    <li key={source.chunkId || index}>
                                        <span title={source.textSnippet}>{source.sourceName}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}