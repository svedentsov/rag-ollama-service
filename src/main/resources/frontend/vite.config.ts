import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import svgr from 'vite-plugin-svgr'

export default defineConfig({
  plugins: [
    react(),
    svgr()
  ],
  build: {
    // Указываем Vite складывать собранные файлы прямо в директорию статики Spring Boot
    outDir: '../static/assets',
    // Очищать директорию перед сборкой
    emptyOutDir: true,
    // Генерировать manifest.json для отслеживания имен файлов с хэшами
    manifest: true,
    // !!! ИСПРАВЛЕНИЕ: Явно указываем точку входа для сборки !!!
    rollupOptions: {
      input: {
        main: './src/main.tsx',
      },
    },
  }
})
