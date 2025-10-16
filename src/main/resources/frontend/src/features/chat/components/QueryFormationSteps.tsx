import React, { FC } from 'react';
import { QueryFormationStep } from '../types';
import { Bot, Search, ListPlus, CornerDownLeft } from 'lucide-react';
import styles from './QueryFormationSteps.module.css';

/**
 * @interface QueryFormationStepsProps
 * @description Пропсы для компонента QueryFormationSteps.
 */
interface QueryFormationStepsProps {
  /** @param {QueryFormationStep[]} steps - Массив шагов формирования запроса для визуализации. */
  steps: QueryFormationStep[];
}

/**
 * Карта для сопоставления имен шагов конвейера с соответствующими иконками.
 */
const StepIcons: Record<string, FC<{ size: number }>> = {
  'HyDEAgent': Bot,
  'QueryTransformationAgent': Search,
  'MultiQueryAgent': ListPlus,
  'StepBackQueryAgent': CornerDownLeft,
};

/**
 * Компонент для визуализации пошаговой истории трансформации RAG-запроса.
 * Представляет процесс улучшения запроса в виде вертикального таймлайна,
 * что помогает в отладке и понимании работы RAG-конвейера.
 * @param {QueryFormationStepsProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const QueryFormationSteps: FC<QueryFormationStepsProps> = ({ steps }) => {
  return (
    <details className={styles.details}>
      <summary className={styles.summary}>
        <Bot size={16} />
        <span>Как был сформирован запрос</span>
      </summary>
      <div className={styles.stepsContainer}>
        {steps.map((step, index) => {
          const Icon = StepIcons[step.stepName] || Bot;
          const result = step.result;

          return (
            <div key={index} className={styles.step}>
              <div className={styles.stepHeader}>
                <div className={styles.stepIcon}><Icon size={14} /></div>
                <h5 className={styles.stepName}>{step.stepName}</h5>
              </div>
              <p className={styles.stepDescription}>{step.description}</p>
              <pre className={styles.stepResult}>
                <code>
                  {Array.isArray(result) ? result.join('\n') : String(result)}
                </code>
              </pre>
            </div>
          );
        })}
      </div>
    </details>
  );
};
