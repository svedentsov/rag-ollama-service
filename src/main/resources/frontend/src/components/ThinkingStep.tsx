import React, { FC } from 'react';
import { CheckCircle } from 'lucide-react';
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
 * Отображает один шаг в процессе "мышления" AI.
 * Показывает спиннер для выполняющегося шага и иконку-галочку для завершенного.
 * @param {ThinkingStepProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент одного шага.
 */
export const ThinkingStep: FC<ThinkingStepProps> = ({ step }) => {
    const isCompleted = step.status === 'COMPLETED';
    return (
        <div className={`${styles.step} ${isCompleted ? styles.completed : ''}`}>
            <div className={styles.stepIcon}>
                {isCompleted ? <CheckCircle size={16} /> : <div className={styles.stepSpinner} />}
            </div>
            <span className={styles.stepName}>{step.name}</span>
        </div>
    );
};
