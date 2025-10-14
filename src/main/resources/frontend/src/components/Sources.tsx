import React, { FC } from 'react';
import { Message } from '../types';
import styles from './Sources.module.css';

/**
 * @interface SourcesProps
 * @description Пропсы для компонента отображения источников.
 */
interface SourcesProps {
  /** @param {Message} message - Объект сообщения, содержащий источники. */
  message: Message;
}

/**
 * Компонент, отвечающий исключительно за рендеринг сетки с карточками источников.
 * @param {SourcesProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если нет источников.
 */
export const Sources: FC<SourcesProps> = ({ message }) => {
  const hasSources = message.sources && message.sources.length > 0;

  if (!hasSources) {
    return null;
  }

  return (
    <div className={styles.sourcesBlock}>
      <div className={styles.sourcesHeader}>
        <h4 className={styles.sourcesTitle}>Источники:</h4>
      </div>
      <div className={styles.sourcesGrid}>
        {message.sources!.map((source, index) => (
          <a
            key={source.chunkId || index}
            href={source.metadata.url || '#'}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.sourceCard}
            title={source.textSnippet}
          >
            <div className={styles.sourceHeader}>
              <span className={styles.sourceIndex}>{index + 1}</span>
              <span className={styles.sourceName}>{source.sourceName}</span>
            </div>
            <p className={styles.sourceSnippet}>{source.textSnippet}</p>
          </a>
        ))}
      </div>
    </div>
  );
};
