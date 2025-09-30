<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Agent Platform</title>

    <#--
      Эта секция теперь едина и для разработки, и для продакшена.
      Vite Dev Server автоматически перехватит <script type="module" src="/src/main.tsx">
      и внедрит все необходимое для HMR и React Fast Refresh.
      В production-сборке Spring Boot будет отдавать статические файлы из /static/assets.
    -->
    <#if isDevelopmentMode>
        <#--
          В режиме разработки Vite Dev Server сам инжектирует все необходимые скрипты,
          когда видит ссылку на точку входа /src/main.tsx.
          Никаких ручных вставок @vite/client или @react-refresh не требуется.
        -->
        <script type="module" src="http://localhost:5173/src/main.tsx"></script>
    <#else>
        <#--
          Режим Production: подключаем скомпилированные и версионированные ассеты.
          Пути абсолютные, так как Spring Boot будет отдавать их из директории static.
        -->
        <link rel="stylesheet" href="/assets/main.css">
        <script type="module" defer src="/assets/main.js"></script>
    </#if>
</head>
<body>
    <noscript>You need to enable JavaScript to run this app.</noscript>
    <div id="app-root">
        <#--
          Этот скелетный загрузчик будет виден пользователю короткое время,
          пока React-приложение загружается и инициализируется.
          Это улучшает воспринимаемую производительность (Perceived Performance).
        -->
        <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #fff; color: #333;">
            <div style="width: 2rem; height: 2rem; border: 0.25em solid currentColor; border-right-color: transparent; border-radius: 50%; animation: spinner-border .75s linear infinite;" role="status">
                <span style="position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border-width: 0;">Загрузка...</span>
            </div>
        </div>
        <style>
            @keyframes spinner-border {
              to { transform: rotate(360deg); }
            }
        </style>
    </div>
</body>
</html>
