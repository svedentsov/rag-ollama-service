import React, { FC } from 'react';
import { FileText } from 'lucide-react';
import { CodeBlock } from './CodeBlock';
import styles from './QueryFormationSteps.module.css'; // Переиспользуем стили

/**
 * @interface PromptInspectorProps
 * @description Пропсы для компонента PromptInspector.
 */
interface PromptInspectorProps {
  /** @param {string} prompt - Полный текст промпта для отображения. */
  prompt: string;
}

/**
 * Компонент, который отображает финальный промпт внутри раскрывающегося
 * блока `<details>`, унифицируя UX с `QueryFormationSteps`.
 * @param {PromptInspectorProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const PromptInspector: FC<PromptInspectorProps> = ({ prompt }) => {
  return (
    <details className={styles.details}>
      <summary className={styles.summary}>
        <FileText size={16} />
        <span>Показать промпт</span>
      </summary>
      <div className={styles.stepsContainer}>
        <CodeBlock language="markdown">{prompt}</CodeBlock>
      </div>
    </details>
  );
};
