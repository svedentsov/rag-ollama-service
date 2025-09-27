<#-- C:\Users\svede\IdeaProjects\rag-ollama-service\src\main\resources\templates\_layout.ftl -->
<#macro page title>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} | AI Agent Platform</title>

    <#if isDevelopmentMode?? && isDevelopmentMode>
        <#-- Режим разработки: Правильная интеграция с Vite Dev Server для React Fast Refresh -->
        <script type="module">
          import RefreshRuntime from "http://localhost:5173/@react-refresh";
          RefreshRuntime.injectIntoGlobalHook(window);
          window.$RefreshReg$ = () => {};
          window.$RefreshSig$ = () => (type) => type;
          window.__vite_plugin_react_preamble_installed__ = true;
        </script>
        <script type="module" src="http://localhost:5173/@vite/client"></script>
        <script type="module" src="http://localhost:5173/src/main.tsx"></script>
    <#else>
        <#-- Режим Production: подключаем скомпилированные ассеты -->
        <link rel="stylesheet" href="/assets/main.css">
        <script type="module" defer src="/assets/main.js"></script>
    </#if>
</head>
<body class="bg-light">
    <#-- *** КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Единая точка входа для всего React-приложения *** -->
    <div id="app-root">
        <#--
          Этот скелетный загрузчик будет виден пользователю короткое время,
          пока Vite/React загружает и инициализирует все приложение.
        -->
        <div style="display: flex; justify-content: center; align-items: center; height: 100vh;">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">Загрузка приложения...</span>
            </div>
        </div>
    </div>
</body>
</html>
</#macro>
