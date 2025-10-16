import React, { FC } from 'react';
import { FileText } from 'lucide-react';
import { PromptInspector } from './PromptInspector';
import { QueryFormationSteps } from './QueryFormationSteps';
import { Message } from '../types';
import styles from './InspectorToolbar.module.css';

/**
 * @interface InspectorToolbarProps
 * @description Пропсы для компонента тулбара инспекции.
 */
interface InspectorToolbarProps {
  /** @param {Message} message - Объект сообщения, содержащий метаданные для анализа. */
  message: Message;
}

/**
 * Компонент-контейнер для всех инструментов анализа и объяснения ("XAI").
 * Группирует в одном месте `QueryFormationSteps` и `PromptInspector` для
 * обеспечения семантической близости и чистоты интерфейса.
 *
 * @param {InspectorToolbarProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если нет данных для инспекции.
 */
export const InspectorToolbar: FC<InspectorToolbarProps> = ({ message }) => {
  const hasHistory = message.queryFormationHistory && message.queryFormationHistory.length > 0;
  const hasPrompt = !!message.finalPrompt;

  if (!hasHistory && !hasPrompt) {
    return null;
  }

  return (
    <div className={styles.toolbar}>
      {hasHistory && <QueryFormationSteps steps={message.queryFormationHistory!} />}
      {hasPrompt && <PromptInspector prompt={message.finalPrompt!} />}
    </div>
  );
};
