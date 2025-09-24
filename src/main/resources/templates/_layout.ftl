<#macro page title>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>${title} | AI Agent Platform</title>
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <!-- Prism JS for Syntax Highlighting -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js" defer></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js" defer></script>
    <style>
        :root {
            --primary-color: #1a73e8; --border-color: #e0e0e0; --bg-light: #f4f7f6;
            --text-color: #333; --sidebar-width: 280px;
        }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: var(--bg-light); margin: 0; display: flex; height: 100vh; color: var(--text-color); }
        .main-wrapper { display: flex; flex: 1; overflow: hidden; }
        .sidebar { width: var(--sidebar-width); background-color: #ffffff; border-right: 1px solid var(--border-color); padding: 1rem; display: flex; flex-direction: column; overflow-y: auto; }
        .chat-list-header {
            font-size: 0.9rem; font-weight: 600; color: #666;
            margin-top: 1rem; margin-bottom: 0.5rem; text-transform: uppercase;
            display: flex; justify-content: space-between; align-items: center;
        }
        .chat-list { list-style: none; padding: 0; margin: 0; }
        .chat-list li { display: flex; align-items: center; justify-content: space-between; border-radius: 6px; }
        .chat-list li:hover { background-color: #f0f0f0; }
        .chat-list li:hover .chat-actions { opacity: 1; }
        .chat-list a { flex-grow: 1; padding: 0.6rem 0.8rem; text-decoration: none; color: var(--text-color); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .chat-list a.active { background-color: var(--primary-color); color: white; border-radius: 6px; }
        .chat-list li.active-item { background-color: var(--primary-color); }
        .chat-list li.active-item a { color: white; }
        .chat-actions { display: flex; opacity: 0; transition: opacity 0.2s; padding-right: 8px; }
        .chat-action-btn { background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 4px; color: #555; }
        li.active-item .chat-action-btn { color: white; }
        .edit-input { width: 100%; box-sizing: border-box; padding: 0.5rem; border: 1px solid var(--primary-color); border-radius: 4px; }
        .content-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
        .new-chat-btn {
            background: none; border: none; cursor: pointer; font-size: 1.5rem;
            color: #555; padding: 0 8px; line-height: 1;
        }
        .new-chat-btn:hover { color: var(--primary-color); }
    </style>
</head>
<body>
<div class="main-wrapper">
    <nav class="sidebar">
        <div>
            <h2 class="chat-list-header">
                <span>Чаты</span>
                <button id="new-chat-btn" class="new-chat-btn" title="Создать новый чат">+</button>
            </h2>
            <ul class="chat-list" id="chat-list">
                <li><a href="#">Загрузка...</a></li>
            </ul>
        </div>
    </nav>
    <main class="content-area">
        <#nested>
    </main>
</div>

<script>
<#noparse>
document.addEventListener('DOMContentLoaded', function() {
    const chatList = document.getElementById('chat-list');
    const newChatBtn = document.getElementById('new-chat-btn');

    async function fetchAndRenderChats() {
        try {
            const response = await fetch('/api/v1/chats');
            if (!response.ok) throw new Error('Failed to fetch chats');
            const chats = await response.json();
            chatList.innerHTML = '';
            const currentSessionId = new URLSearchParams(window.location.search).get('sessionId');

            if (chats.length === 0) {
                const li = document.createElement('li');
                li.textContent = "Нет активных чатов";
                li.style.color = '#888';
                li.style.padding = '0.6rem 0.8rem';
                chatList.appendChild(li);
            } else {
                chats.forEach(chat => {
                    const li = document.createElement('li');
                    li.dataset.sessionId = chat.sessionId;
                    li.dataset.chatName = chat.chatName;

                    const a = document.createElement('a');
                    a.href = `/chat?sessionId=${chat.sessionId}`;
                    a.textContent = chat.chatName;
                    a.title = chat.chatName;

                    if (chat.sessionId === currentSessionId) {
                        li.classList.add('active-item');
                    }

                    const actionsDiv = document.createElement('div');
                    actionsDiv.className = 'chat-actions';
                    actionsDiv.innerHTML = `
                        <button class="chat-action-btn edit-btn" title="Редактировать">✎</button>
                        <button class="chat-action-btn delete-btn" title="Удалить">🗑</button>
                    `;

                    li.appendChild(a);
                    li.appendChild(actionsDiv);
                    chatList.appendChild(li);
                });
            }
        } catch (error) {
            console.error("Error rendering chats:", error);
            chatList.innerHTML = '<li><a href="#">Ошибка загрузки</a></li>';
        }
    }

    newChatBtn.addEventListener('click', async () => {
        try {
            const headers = { 'Content-Type': 'application/json' };
            const response = await fetch('/api/v1/chats', {
                method: 'POST',
                headers: headers
            });
            if (!response.ok) throw new Error('Failed to create new chat');
            const newChat = await response.json();
            window.location.href = `/chat?sessionId=${newChat.sessionId}`;
        } catch (error) {
            console.error("Error creating new chat:", error);
            alert('Не удалось создать новый чат.');
        }
    });

    chatList.addEventListener('click', function(e) {
        const target = e.target;
        if (target.classList.contains('edit-btn')) {
            handleEditChat(target.closest('li'));
        }
        if (target.classList.contains('delete-btn')) {
            handleDeleteChat(target.closest('li'));
        }
    });

    function handleEditChat(listItem) {
        const sessionId = listItem.dataset.sessionId;
        const currentName = listItem.dataset.chatName;
        const link = listItem.querySelector('a');

        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'edit-input';
        input.value = currentName;

        listItem.replaceChild(input, link);
        input.focus();
        input.select();

        const saveChanges = async () => {
            const newName = input.value.trim();
            if (newName && newName !== currentName) {
                try {
                    const headers = { 'Content-Type': 'application/json' };
                    await fetch(`/api/v1/chats/${sessionId}`, {
                        method: 'PUT',
                        headers: headers,
                        body: JSON.stringify({ newName: newName })
                    });
                } catch (err) {
                    console.error("Error updating chat name:", err);
                }
            }
            fetchAndRenderChats();
        };

        input.addEventListener('blur', saveChanges);
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') input.blur();
            if (e.key === 'Escape') fetchAndRenderChats();
        });
    }

    async function handleDeleteChat(listItem) {
        const sessionId = listItem.dataset.sessionId;
        const chatName = listItem.dataset.chatName;

        if (confirm(`Вы уверены, что хотите удалить чат "${chatName}"?`)) {
            try {
                const response = await fetch(`/api/v1/chats/${sessionId}`, {
                    method: 'DELETE'
                });
                if (!response.ok) throw new Error('Failed to delete chat');

                const currentSessionId = new URLSearchParams(window.location.search).get('sessionId');
                if (sessionId === currentSessionId) {
                    window.location.href = '/';
                } else {
                    fetchAndRenderChats();
                }
            } catch (error) {
                console.error("Error deleting chat:", error);
                alert("Не удалось удалить чат.");
            }
        }
    }

    fetchAndRenderChats();
});
</#noparse>
</script>
</body>
</html>
</#macro>
