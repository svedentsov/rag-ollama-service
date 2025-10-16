import React, { useState, FC, memo } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { darcula } from 'react-syntax-highlighter/dist/esm/styles/prism';
import toast from 'react-hot-toast';
import { Copy, Check } from 'lucide-react';
import styles from './CodeBlock.module.css';

/**
 * @interface CodeBlockProps
 * @description Пропсы для компонента CodeBlock.
 */
interface CodeBlockProps {
  /** @param {string} language - Язык программирования для подсветки синтаксиса. */
  language: string;
  /** @param {string} children - Код для отображения. */
  children: string;
}

/**
 * Компонент для отображения блоков кода с подсветкой синтаксиса и кнопкой копирования.
 * Оптимизирован с помощью React.memo для предотвращения ненужных перерисовок,
 * так как его вывод зависит только от пропсов.
 * @param {CodeBlockProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const CodeBlock: FC<CodeBlockProps> = memo(({ language, children }) => {
  const [isCopied, setIsCopied] = useState(false);

  /**
   * Обработчик нажатия на кнопку "Копировать".
   */
  const handleCopy = () => {
    navigator.clipboard.writeText(children).then(() => {
      setIsCopied(true);
      toast.success('Скопировано в буфер обмена!');
      setTimeout(() => setIsCopied(false), 2000); // Сбрасываем состояние через 2 секунды
    }, () => {
      toast.error('Не удалось скопировать.');
    });
  };

  const Icon = isCopied ? Check : Copy;

  return (
    <div className={styles.codeBlockWrapper}>
      <div className={styles.codeHeader}>
        <span>{language}</span>
        <button
          className={styles.btnCopy}
          onClick={handleCopy}
          title="Скопировать код"
          aria-label={isCopied ? "Код скопирован" : "Скопировать код"}
          aria-live="polite"
        >
          <Icon size={14} /> {isCopied ? 'Скопировано!' : 'Копировать'}
        </button>
      </div>
      <SyntaxHighlighter
        style={darcula}
        language={language}
        PreTag="div"
        className={styles.syntaxHighlighter}
      >
        {children}
      </SyntaxHighlighter>
    </div>
  );
});
