import React, { FC } from 'react';
import { useStreamingStore } from '../state/streamingStore';
import { ThinkingStep } from './ThinkingStep';
import { StatusIndicator } from './StatusIndicator';
import styles from './ThinkingThoughts.module.css';

/**
 * @interface ThinkingThoughtsProps
 * @description Пропсы для компонента, визуализирующего процесс "мышления" AI-агента.
 */
interface ThinkingThoughtsProps {
  /** @param {string} assistantMessageId - ID сообщения ассистента, для которого отображается статус. */
  assistantMessageId: string;
}

/**
 * Компонент, который визуализирует процесс "мышления" AI-агента.
 * Он подписывается на `useStreamingStore` и отображает анимированный
 * список шагов по мере их выполнения для конкретной задачи.
 *
 * @param {ThinkingThoughtsProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент.
 */
export const ThinkingThoughts: FC<ThinkingThoughtsProps> = ({ assistantMessageId }) => {
    const taskState = useStreamingStore((state) => state.activeStreams.get(assistantMessageId));

    if (!taskState) {
        return null;
    }

    const steps = Array.from(taskState.thinkingSteps.values());

    // Если есть шаги "мышления", отображаем их.
    if (steps.length > 0) {
        return (
            <div className={styles.thinkingContainer}>
                <div className={styles.header}>
                    <div className={styles.spinner}></div>
                    <span>Выполняю план...</span>
                </div>
                <div className={styles.stepsList}>
                    {steps.map((step) => (
                        <ThinkingStep key={step.name} step={step} />
                    ))}
                </div>
            </div>
        );
    }

    // Если шагов еще нет, но есть текстовый статус, показываем его.
    if (taskState.statusText) {
        return (
            <div className={styles.thinkingContainer}>
                <StatusIndicator status={taskState.statusText} startTime={taskState.startTime} />
            </div>
        );
    }

    // Fallback по умолчанию (самое начало стрима).
    return (
        <div className={styles.thinkingContainer}>
            <div className={styles.initialThinking}>
                <div className={styles.spinner}></div>
                <span>Анализирую ваш вопрос...</span>
            </div>
        </div>
    );
};
