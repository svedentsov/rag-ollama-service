import React, { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { darcula } from 'react-syntax-highlighter/dist/esm/styles/prism';
import toast from 'react-hot-toast';
import { Copy, Check } from 'lucide-react';
import styles from './CodeBlock.module.css';

/**
 * Пропсы для компонента CodeBlock.
 */
interface CodeBlockProps {
  /** Язык программирования для подсветки синтаксиса. */
  language: string;
  /** Код для отображения. */
  children: string;
}

/**
 * Компонент для отображения блоков кода с подсветкой синтаксиса и кнопкой копирования.
 * Оптимизирован с помощью React.memo для предотвращения ненужных перерисовок.
 * @param {CodeBlockProps} props - Пропсы компонента.
 */
export const CodeBlock: React.FC<CodeBlockProps> = React.memo(({ language, children }) => {
  const [isCopied, setIsCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(children).then(() => {
      setIsCopied(true);
      toast.success('Скопировано в буфер обмена!');
      setTimeout(() => setIsCopied(false), 2000);
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
          aria-label="Скопировать код"
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
