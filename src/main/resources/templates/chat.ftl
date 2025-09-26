<#import "_layout.ftl" as layout>

<@layout.page title="Чат">
    <#--
      Этот div - единственное, что рендерит FreeMarker на этой странице.
      Он служит "точкой монтирования" для нашего React-приложения.
      Мы безопасно передаем sessionId в data-атрибут, откуда React его заберет.
    -->
    <div id="chat-widget-root"
         data-session-id="${sessionId!''}"
         class="flex-grow-1 p-3"
         style="height: 100%; overflow: hidden;">

        <#--
          Этот скелетный загрузчик будет виден пользователю короткое время,
          пока Vite/React загружает и инициализирует основной компонент чата.
          Это улучшает воспринимаемую производительность (Perceived Performance).
        -->
        <div class="d-flex align-items-center justify-content-center h-100">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">Загрузка приложения...</span>
            </div>
        </div>
    </div>
</@layout.page>