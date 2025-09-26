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
        <script type="module" src="/assets/main.js"></script>
    </#if>

</head>
<body class="bg-light">
<div class="d-flex vh-100">
    <#--
      ИСПРАВЛЕНИЕ: Статическая разметка навигации полностью удалена.
      Остался только пустой div, который React будет использовать для монтирования сайдбара.
    -->
    <div id="sidebar-root" style="width: 280px; flex-shrink: 0;"></div>

    <main class="flex-grow-1 d-flex flex-column" style="overflow: hidden;">
        <#nested>
    </main>
</div>
</body>
</html>
</#macro>
