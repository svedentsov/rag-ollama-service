import React, { FC } from 'react';
import { Message, SourceCitation } from '../../../types';
import styles from './Sources.module.css';

/**
 * @interface SourcesProps
 * @description Пропсы для компонента отображения источников.
 */
interface SourcesProps {
  /** @param {Message} message - Объект сообщения, содержащий источники, подтверждающие ответ. */
  message: Message;
  /** @param {(source: SourceCitation) => void} onViewSource - Колбэк для открытия просмотра источника. */
  onViewSource: (source: SourceCitation) => void;
}

/**
 * Презентационный компонент, отвечающий за рендеринг сетки с карточками источников (цитат).
 * @param {SourcesProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если у сообщения нет источников.
 */
export const Sources: FC<SourcesProps> = ({ message, onViewSource }) => {
  const hasSources = message.sources && message.sources.length > 0;

  if (!hasSources) {
    return null;
  }

  return (
    <div className={styles.sourcesContainer}>
      <div className={styles.sourcesHeader}>
        <h4 className={styles.sourcesTitle}>Источники:</h4>
      </div>
      <div className={styles.sourcesGrid}>
        {message.sources!.map((source, index) => (
          <button
            key={source.chunkId || index}
            onClick={() => onViewSource(source)}
            className={styles.sourceCard}
            title={source.textSnippet}
          >
            <div className={styles.sourceHeader}>
              <span className={styles.sourceIndex}>{index + 1}</span>
              <span className={styles.sourceName}>{source.sourceName}</span>
            </div>
            <p className={styles.sourceSnippet}>{source.textSnippet}</p>
          </button>
        ))}
      </div>
    </div>
  );
};
