<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Agent Chat</title>
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: #f4f7f6;
            margin: 0;
            display: flex;
            flex-direction: column;
            height: 100vh;
            color: #333;
        }
        .header {
            background-color: #ffffff;
            padding: 1rem;
            border-bottom: 1px solid #e0e0e0;
            text-align: center;
            font-size: 1.2rem;
            font-weight: 600;
            color: #1a73e8;
        }
        #chat-container {
            flex: 1;
            overflow-y: auto;
            padding: 1rem;
            display: flex;
            flex-direction: column;
        }
        .message {
            max-width: 70%;
            padding: 0.75rem 1rem;
            border-radius: 18px;
            margin-bottom: 0.5rem;
            line-height: 1.4;
        }
        .user-message {
            background-color: #1a73e8;
            color: white;
            align-self: flex-end;
            border-bottom-right-radius: 4px;
        }
        .assistant-message {
            background-color: #ffffff;
            color: #333;
            align-self: flex-start;
            border: 1px solid #e0e0e0;
            border-bottom-left-radius: 4px;
        }
        .assistant-message .sources {
            margin-top: 10px;
            border-top: 1px solid #eee;
            padding-top: 10px;
            font-size: 0.8rem;
        }
        .assistant-message .sources strong {
            display: block;
            margin-bottom: 5px;
        }
        .assistant-message .sources ul {
            padding-left: 15px;
            margin: 0;
        }
        .assistant-message .sources li {
            margin-bottom: 5px;
        }
        .typing-indicator {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #999;
            animation: typing 1s infinite;
        }
        .typing-indicator:nth-child(2) { animation-delay: 0.2s; }
        .typing-indicator:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typing {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-4px); }
        }
        #chat-form {
            display: flex;
            padding: 1rem;
            background-color: #ffffff;
            border-top: 1px solid #e0e0e0;
        }
        #message-input {
            flex: 1;
            padding: 0.75rem;
            border: 1px solid #ccc;
            border-radius: 20px;
            margin-right: 0.5rem;
            font-size: 1rem;
        }
        #message-input:focus {
            outline: none;
            border-color: #1a73e8;
        }
        button {
            padding: 0.75rem 1.5rem;
            border: none;
            background-color: #1a73e8;
            color: white;
            border-radius: 20px;
            cursor: pointer;
            font-size: 1rem;
            font-weight: 500;
        }
        button:disabled {
            background-color: #9e9e9e;
            cursor: not-allowed;
        }
    </style>
</head>
<body>

<div class="header">Autonomous AI Agent Platform</div>
<div id="chat-container"></div>
<form id="chat-form">
    <input type="text" id="message-input" placeholder="Спросите что-нибудь..." autocomplete="off" required>
    <button type="submit">Отправить</button>
</form>

<script>
<#noparse>
    // Ждем, пока весь HTML-документ будет загружен и готов к взаимодействию
    document.addEventListener('DOMContentLoaded', function() {
        const chatForm = document.getElementById('chat-form');
        const messageInput = document.getElementById('message-input');
        const chatContainer = document.getElementById('chat-container');
        const sendButton = chatForm.querySelector('button');

        let sessionId = null;

        chatForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const message = messageInput.value.trim();
            if (!message) return;

            appendMessage(message, 'user');
            messageInput.value = '';
            sendButton.disabled = true;

            const assistantMsgContainer = appendMessage('', 'assistant');
            const typingIndicator = showTypingIndicator(assistantMsgContainer);

            let assistantMarkdownContent = '';

            try {
                const response = await fetch('/api/v1/orchestrator/ask-stream', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream'
                    },
                    body: JSON.stringify({ query: message, sessionId: sessionId })
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                let assistantContentDiv = document.createElement('div');
                assistantMsgContainer.appendChild(assistantContentDiv);

                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;

                    hideTypingIndicator(typingIndicator);

                    buffer += decoder.decode(value, { stream: true });
                    const lines = buffer.split('\n\n');

                    buffer = lines.pop();

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            const jsonData = line.substring(5).trim();
                            try {
                                const parsedData = JSON.parse(jsonData);
                                assistantMarkdownContent = processStreamEvent(parsedData, assistantMsgContainer, assistantContentDiv, assistantMarkdownContent);
                            } catch (err) {
                                console.error('Error parsing JSON from stream:', jsonData, err);
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('Error fetching stream:', error);
                hideTypingIndicator(typingIndicator);
                assistantMsgContainer.innerHTML = `<p style="color: red;">Ошибка: ${error.message}</p>`;
            } finally {
                sendButton.disabled = false;
                messageInput.focus();
            }
        });

        function processStreamEvent(data, container, contentDiv, currentMarkdown) {
            switch (data.type) {
                case 'content':
                    currentMarkdown += data.text;
                    contentDiv.innerHTML = marked.parse(currentMarkdown);
                    break;
                case 'sources':
                    appendSources(data.sources, container);
                    break;
                case 'done':
                    console.log('Stream finished:', data.message);
                    break;
                case 'error':
                    contentDiv.innerHTML += `<p style="color: red;">Ошибка: ${data.message}</p>`;
                    break;
                case 'bugAnalysis':
                    contentDiv.innerHTML = generateBugAnalysisHtml(data.analysis);
                    break;
            }
            scrollToBottom();
            return currentMarkdown;
        }

        function generateBugAnalysisHtml(analysis) {
            let bugHtml = '<h4>Анализ Баг-репорта</h4>';
            bugHtml += `<p><strong>Вердикт:</strong> ${analysis.isDuplicate ? 'Возможный дубликат' : 'Уникальный'}</p>`;
            bugHtml += `<h5>Улучшенное описание:</h5>`;
            bugHtml += `<p><strong>Заголовок:</strong> ${analysis.improvedDescription.title}</p>`;
            bugHtml += '<strong>Шаги:</strong><ul>' + analysis.improvedDescription.stepsToReproduce.map(s => `<li>${s}</li>`).join('') + '</ul>';
            bugHtml += `<p><strong>Ожидаемый результат:</strong> ${analysis.improvedDescription.expectedBehavior}</p>`;
            bugHtml += `<p><strong>Фактический результат:</strong> ${analysis.improvedDescription.actualBehavior}</p>`;
            if(analysis.duplicateCandidates && analysis.duplicateCandidates.length > 0) {
                 bugHtml += '<strong>Кандидаты в дубликаты:</strong><ul>' + analysis.duplicateCandidates.map(c => `<li>${c}</li>`).join('') + '</ul>';
            }
            return bugHtml;
        }

        function appendMessage(text, type) {
            const messageDiv = document.createElement('div');
            messageDiv.classList.add('message', `${type}-message`);
            messageDiv.innerHTML = marked.parse(text);
            chatContainer.appendChild(messageDiv);
            scrollToBottom();
            return messageDiv;
        }

        function showTypingIndicator(container) {
            const indicatorContainer = document.createElement('div');
            indicatorContainer.innerHTML = '<span class="typing-indicator"></span><span class="typing-indicator"></span><span class="typing-indicator"></span>';
            container.appendChild(indicatorContainer);
            scrollToBottom();
            return indicatorContainer;
        }

        function hideTypingIndicator(indicator) {
            if (indicator) {
                indicator.remove();
            }
        }

        function appendSources(sources, container) {
            if (!sources || sources.length === 0) return;
            const sourcesDiv = document.createElement('div');
            sourcesDiv.className = 'sources';
            let sourcesHtml = '<strong>Источники:</strong><ul>';
            sources.forEach(source => {
                 sourcesHtml += `<li>${source.sourceName} (ID: ${source.chunkId})</li>`;
            });
            sourcesHtml += '</ul>';
            sourcesDiv.innerHTML = sourcesHtml;
            container.appendChild(sourcesDiv);
        }

        function scrollToBottom() {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    });
</#noparse>
</script>

</body>
</html>
