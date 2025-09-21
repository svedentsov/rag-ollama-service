<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Вход в систему</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f7f6; }
        .login-container { background: white; padding: 2rem 3rem; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 320px; text-align: center; }
        h1 { color: #1a73e8; margin-bottom: 1.5rem; }
        .form-group { margin-bottom: 1rem; text-align: left; }
        label { display: block; margin-bottom: 0.5rem; font-weight: 500; color: #555; }
        input { width: 100%; padding: 0.75rem; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        button { width: 100%; padding: 0.75rem; border: none; background-color: #1a73e8; color: white; border-radius: 4px; cursor: pointer; font-size: 1rem; font-weight: 600; margin-top: 1rem; }
        .alert { padding: 0.75rem; margin-bottom: 1rem; border-radius: 4px; }
        .alert-danger { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .alert-success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
    </style>
</head>
<body>
    <div class="login-container">
        <h1>Вход в систему</h1>

        <#-- Проверяем наличие флага 'error' в модели -->
        <#if error??>
            <div class="alert alert-danger">Неверное имя пользователя или пароль.</div>
        </#if>

        <#-- Проверяем наличие флага 'logout' в модели -->
        <#if logout??>
            <div class="alert alert-success">Вы успешно вышли из системы.</div>
        </#if>

        <form action="/login" method="post">
            <div class="form-group">
                <label for="username">Имя пользователя:</label>
                <input type="text" id="username" name="username" required autofocus>
            </div>
            <div class="form-group">
                <label for="password">Пароль:</label>
                <input type="password" id="password" name="password" required>
            </div>

            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <button type="submit">Войти</button>
        </form>
    </div>
</body>
</html>
