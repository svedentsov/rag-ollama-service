<#import "_layout.ftl" as layout>

<@layout.page title="Чат">
    <style>
        #chat-container { flex: 1; overflow-y: auto; padding: 1rem; display: flex; flex-direction: column; }
        .message { max-width: 80%; padding: 0.75rem 1rem; border-radius: 18px; margin-bottom: 0.5rem; line-height: 1.5; word-wrap: break-word; }
        .user-message { background-color: var(--primary-color); color: white; align-self: flex-end; border-bottom-right-radius: 4px; }
        .assistant-message { background-color: #ffffff; color: var(--text-color); align-self: flex-start; border: 1px solid var(--border-color); border-bottom-left-radius: 4px; }
        .assistant-message p { margin: 0 0 0.5em; }
        .assistant-message p:last-child { margin-bottom: 0; }
        .assistant-message ul, .assistant-message ol { margin: 0.5em 0; padding-left: 1.5em; }
        .assistant-message li { margin-bottom: 0.25em; }
        .assistant-message pre { margin: 1em 0 !important; border-radius: 8px !important; }
        .assistant-message code[class*="language-"] { font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace; font-size: 0.9em; }
        .assistant-message .sources { margin-top: 10px; border-top: 1px solid #eee; padding-top: 10px; font-size: 0.8rem; }
        .assistant-message .sources strong { display: block; margin-bottom: 5px; }
        .assistant-message .sources ul { padding-left: 15px; margin: 0; }
        .assistant-message .sources li { margin-bottom: 5px; }
        .typing-indicator-container { display: flex; gap: 4px; align-items: center; padding: 0.75rem 1rem; }
        .typing-indicator { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background-color: #999; animation: typing 1s infinite; }
        .typing-indicator:nth-child(2) { animation-delay: 0.2s; }
        .typing-indicator:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typing { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
        #form-container { display: flex; padding: 1rem; background-color: #ffffff; border-top: 1px solid var(--border-color); align-items: center; gap: 0.5rem; }
        #message-input { flex: 1; padding: 0.75rem; border: 1px solid #ccc; border-radius: 20px; font-size: 1rem; }
        #message-input:focus { outline: none; border-color: var(--primary-color); }
        #form-container button { padding: 0.75rem 1.5rem; border: none; color: white; border-radius: 20px; cursor: pointer; font-size: 1rem; font-weight: 500; }
        #send-btn { background-color: var(--primary-color); }
        #send-btn:disabled { background-color: #9e9e9e; cursor: not-allowed; }
        #stop-btn { background-color: #d93025; display: none; }
    </style>

    <div id="chat-container"></div>

    <div id="form-container">
        <input type="text" id="message-input" placeholder="Спросите что-нибудь..." autocomplete="off" required>
        <button id="send-btn" type="submit" form="chat-form">Отправить</button>
        <button id="stop-btn" type="button">Остановить</button>
    </div>
    <form id="chat-form" style="display: none;"></form>

    <script>
        const sessionId = "${sessionId!''}";
        <#noparse>
        document.addEventListener('DOMContentLoaded', function() {
            const chatForm = document.getElementById('chat-form');
            if (!chatForm) return;

            const messageInput = document.getElementById('message-input');
            const chatContainer = document.getElementById('chat-container');
            const sendButton = document.getElementById('send-btn');
            const stopButton = document.getElementById('stop-btn');

            let currentSessionId = sessionId;
            let currentTaskId = null; // Для хранения ID текущей задачи
            let abortController = new AbortController(); // Для отмены fetch

            async function loadChatHistory() {
                if (!currentSessionId) {
                    appendMessage("Чем я могу вам помочь сегодня?", "assistant", true);
                    return;
                }

                const tempMsg = appendMessage('', 'assistant', false);
                const typingIndicator = showTypingIndicator(tempMsg);

                try {
                    const response = await fetch(`/api/v1/chats/${currentSessionId}/messages`);
                    if (!response.ok) {
                        throw new Error('Не удалось загрузить историю чата.');
                    }
                    const messages = await response.json();

                    tempMsg.remove();
                    chatContainer.innerHTML = '';

                    if (messages.length === 0) {
                        appendMessage("Чем я могу вам помочь в этом чате?", "assistant", true);
                    } else {
                        messages.forEach(msg => {
                            appendMessage(msg.content, msg.role.toLowerCase(), true);
                        });
                    }

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

                appendMessage(message, 'user', true);
                messageInput.value = '';
                sendButton.disabled = true;

                const assistantMsgContainer = appendMessage('', 'assistant', false);
                const typingIndicator = showTypingIndicator(assistantMsgContainer);
                let assistantMarkdownContent = '';
                let assistantContentDiv = document.createElement('div');
                assistantMsgContainer.appendChild(assistantContentDiv);

                abortController = new AbortController();

                try {
                    const response = await fetch('/api/v1/orchestrator/ask-stream', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
                        body: JSON.stringify({ query: message, sessionId: currentSessionId || null }),
                        signal: abortController.signal // Привязываем AbortController
                    });

                    if (!response.ok) {
                         const errorText = await response.text();
                         throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                    }

                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();
                    let buffer = '';

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
                    if (error.name !== 'AbortError') { // Не показываем ошибку, если отмена была инициирована пользователем
                        console.error('Error fetching stream:', error);
                        hideTypingIndicator(typingIndicator);
                        assistantContentDiv.innerHTML = `<p style="color: red;">Ошибка: ${error.message}</p>`;
                    }
                } finally {
                    resetUiState();
                }
            });

            stopButton.addEventListener('click', async () => {
                if (!currentTaskId) return;
                stopButton.disabled = true;
                stopButton.textContent = "Остановка...";
                abortController.abort(); // Отменяем fetch на стороне клиента
                try {
                    await fetch(`/api/v1/tasks/${currentTaskId}`, { method: 'DELETE' });
                    appendMessage("Генерация ответа остановлена пользователем.", "assistant", false);
                } catch (error) {
                    console.error("Failed to cancel task:", error);
                    appendMessage("Не удалось остановить задачу.", "assistant", false);
                } finally {
                    resetUiState();
                }
            });

            function processStreamEvent(data, container, contentDiv, currentMarkdown) {
                let updatedMarkdown = currentMarkdown;
                switch (data.type) {
                    case 'task_started':
                        currentTaskId = data.taskId;
                        stopButton.style.display = 'inline-block';
                        sendButton.style.display = 'none';
                        break;
                    case 'content':
                        updatedMarkdown += data.text;
                        contentDiv.innerHTML = marked.parse(updatedMarkdown);
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
                return updatedMarkdown;
            }

            function appendMessage(text, type, shouldHighlight) {
                const messageDiv = document.createElement('div');
                const messageType = type.toLowerCase();
                messageDiv.classList.add('message', `${messageType}-message`);
                if (text) {
                     messageDiv.innerHTML = marked.parse(text);
                }
                chatContainer.appendChild(messageDiv);

                if (shouldHighlight && typeof Prism !== 'undefined') {
                    Prism.highlightAllUnder(messageDiv);
                }

                scrollToBottom();
                return messageDiv;
            }

            function showTypingIndicator(container) {
                const indicatorContainer = document.createElement('div');
                indicatorContainer.className = 'typing-indicator-container';
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
                     sourcesHtml += `<li>${escapeHtml(source.sourceName)} (ID: ${escapeHtml(source.chunkId)})</li>`;
                });
                sourcesHtml += '</ul>';
                sourcesDiv.innerHTML = sourcesHtml;
                container.appendChild(sourcesDiv);
            }

            function generateBugAnalysisHtml(analysis) {
                let bugHtml = '<h4>Анализ Баг-репорта</h4>';
                bugHtml += `<p><strong>Вердикт:</strong> ${analysis.isDuplicate ? 'Возможный дубликат' : 'Уникальный'}</p>`;
                bugHtml += `<h5>Улучшенное описание:</h5>`;
                bugHtml += `<p><strong>Заголовок:</strong> ${escapeHtml(analysis.improvedDescription.title)}</p>`;
                bugHtml += '<strong>Шаги:</strong><ul>' + analysis.improvedDescription.stepsToReproduce.map(s => `<li>${escapeHtml(s)}</li>`).join('') + '</ul>';
                bugHtml += `<p><strong>Ожидаемый результат:</strong> ${escapeHtml(analysis.improvedDescription.expectedBehavior)}</p>`;
                bugHtml += `<p><strong>Фактический результат:</strong> ${escapeHtml(analysis.improvedDescription.actualBehavior)}</p>`;
                if(analysis.duplicateCandidates && analysis.duplicateCandidates.length > 0) {
                     bugHtml += '<strong>Кандидаты в дубликаты:</strong><ul>' + analysis.duplicateCandidates.map(c => `<li>${escapeHtml(c)}</li>`).join('') + '</ul>';
                }
                return bugHtml;
            }

            function resetUiState() {
                sendButton.disabled = false;
                sendButton.style.display = 'inline-block';
                stopButton.style.display = 'none';
                stopButton.disabled = false;
                stopButton.textContent = "Остановить";
                currentTaskId = null;
                messageInput.focus();
                if (typeof Prism !== 'undefined') {
                    Prism.highlightAll();
                }
            }

            function scrollToBottom() {
                chatContainer.scrollTop = chatContainer.scrollHeight;
            }

            function escapeHtml(unsafe) {
                if (!unsafe) return '';
                return unsafe
                     .replace(/&/g, "&amp;")
                     .replace(/</g, "&lt;")
                     .replace(/>/g, "&gt;")
                     .replace(/"/g, "&quot;")
                     .replace(/'/g, "&#039;");
            }

            loadChatHistory();
        });
        </#noparse>
    </script>
</@layout.page>
