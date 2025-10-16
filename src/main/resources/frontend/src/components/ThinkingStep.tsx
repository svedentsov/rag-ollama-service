import { FC } from 'react';
import { ThinkingStep as ThinkingStepType } from '../types';
import styles from './ThinkingThoughts.module.css';

/**
 * @interface ThinkingStepProps
 * @description Пропсы для компонента ThinkingStep.
 */
interface ThinkingStepProps {
  /** @param {ThinkingStepType} step - Объект шага для отображения. */
  step: ThinkingStepType;
}

/**
 * Презентационный компонент, который отображает один шаг в процессе "мышления" AI.
 * Показывает спиннер для выполняющегося шага и иконку-галочку для завершенного.
 * Этот компонент не содержит сложной логики и отвечает только за визуализацию.
 *
 * @param {ThinkingStepProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент одного шага.
 */
export const ThinkingStep: FC<ThinkingStepProps> = ({ step }) => {
    const isCompleted = step.status === 'COMPLETED';
    return (
        <div className={`${styles.step} ${isCompleted ? styles.completed : ''}`}>
            <div className={styles.stepIcon}>
                {isCompleted
                    ? <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    : <div className={styles.stepSpinner} />
                }
            </div>
            <span className={styles.stepName}>{step.name}</span>
        </div>
    );
};
