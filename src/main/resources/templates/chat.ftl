<#import "_layout.ftl" as layout>

<@layout.page title="Чат">
    <style>
        #chat-container { flex: 1; overflow-y: auto; padding: 1rem; display: flex; flex-direction: column; }
        .message { max-width: 70%; padding: 0.75rem 1rem; border-radius: 18px; margin-bottom: 0.5rem; line-height: 1.4; word-wrap: break-word; }
        .user-message { background-color: var(--primary-color); color: white; align-self: flex-end; border-bottom-right-radius: 4px; }
        .assistant-message { background-color: #ffffff; color: var(--text-color); align-self: flex-start; border: 1px solid var(--border-color); border-bottom-left-radius: 4px; }
        .assistant-message p { margin: 0 0 0.5em; }
        .assistant-message p:last-child { margin-bottom: 0; }
        .assistant-message ul, .assistant-message ol { margin: 0.5em 0; padding-left: 1.5em; }
        .assistant-message li { margin-bottom: 0.25em; }
        .assistant-message pre { background-color: #2d2d2d; color: #f8f8f2; padding: 1em; border-radius: 8px; overflow-x: auto; font-family: "Courier New", Courier, monospace; }
        .assistant-message code { font-family: "Courier New", Courier, monospace; background-color: #e0e0e0; padding: 0.2em 0.4em; border-radius: 3px; }
        .assistant-message .sources { margin-top: 10px; border-top: 1px solid #eee; padding-top: 10px; font-size: 0.8rem; }
        .assistant-message .sources strong { display: block; margin-bottom: 5px; }
        .assistant-message .sources ul { padding-left: 15px; margin: 0; }
        .assistant-message .sources li { margin-bottom: 5px; }
        .typing-indicator { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background-color: #999; animation: typing 1s infinite; }
        .typing-indicator:nth-child(2) { animation-delay: 0.2s; }
        .typing-indicator:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typing { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
        #chat-form { display: flex; padding: 1rem; background-color: #ffffff; border-top: 1px solid var(--border-color); }
        #message-input { flex: 1; padding: 0.75rem; border: 1px solid #ccc; border-radius: 20px; margin-right: 0.5rem; font-size: 1rem; }
        #message-input:focus { outline: none; border-color: var(--primary-color); }
        #chat-form button { padding: 0.75rem 1.5rem; border: none; background-color: var(--primary-color); color: white; border-radius: 20px; cursor: pointer; font-size: 1rem; font-weight: 500; }
        #chat-form button:disabled { background-color: #9e9e9e; cursor: not-allowed; }
    </style>

    <div id="chat-container"></div>
    <form id="chat-form">
        <input type="text" id="message-input" placeholder="Спросите что-нибудь..." autocomplete="off" required>
        <button type="submit">Отправить</button>
    </form>

    <script>
        const sessionId = "${sessionId!''}";
    <#noparse>
        document.addEventListener('DOMContentLoaded', function() {
            const chatForm = document.getElementById('chat-form');
            if (!chatForm) return;

            const messageInput = document.getElementById('message-input');
            const chatContainer = document.getElementById('chat-container');
            const sendButton = chatForm.querySelector('button');
            const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            let currentSessionId = sessionId;

            async function loadChatHistory() {
                if (!currentSessionId) {
                    appendMessage("Чем я могу вам помочь сегодня?", "assistant");
                    return;
                }

                const tempMsg = appendMessage('', 'assistant');
                const typingIndicator = showTypingIndicator(tempMsg);

                try {
                    const response = await fetch(`/api/v1/chats/${currentSessionId}/messages`);
                    if (!response.ok) {
                        throw new Error('Не удалось загрузить историю чата.');
                    }
                    const messages = await response.json();

                    tempMsg.remove();

                    chatContainer.innerHTML = ''; // Очищаем контейнер перед рендерингом
                    messages.forEach(msg => {
                        appendMessage(msg.content, msg.role.toLowerCase());
                    });

                } catch (error) {
                    console.error("Error loading chat history:", error);
                    tempMsg.innerHTML = `<p style="color: red;">${error.message}</p>`;
                } finally {
                     scrollToBottom();
                }
            }

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
                            'Accept': 'text/event-stream',
                            [header]: token
                        },
                        body: JSON.stringify({ query: message, sessionId: currentSessionId || null })
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
                                } catch (err) { console.error('Error parsing JSON:', jsonData, err); }
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
                        if (!currentSessionId) {
                           window.location.reload();
                        }
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
                const messageType = type.toLowerCase();
                messageDiv.classList.add('message', `${messageType}-message`);
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
                if (indicator) indicator.remove();
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

            loadChatHistory();
        });
    </#noparse>
    </script>
</@layout.page>
