import React, { FC } from 'react';
import { useStreamingStore } from '../state/streamingStore';
import { ThinkingStep } from './ThinkingStep';
import { StatusIndicator } from './StatusIndicator';
import { useStatusIndicator } from '../hooks/useStatusIndicator';
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
 * @param {ThinkingThoughtsProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент.
 */
export const ThinkingThoughts: FC<ThinkingThoughtsProps> = ({ assistantMessageId }) => {
    const taskState = useStreamingStore((state) => state.activeStreams.get(assistantMessageId));
    const isStreaming = useStreamingStore(state => state.activeStreams.has(assistantMessageId));

    // Хук теперь инкапсулирует таймер и получает только startTime.
    // Компонент ThinkingThoughts больше не перерисовывается каждую секунду.
    const statusIndicatorText = useStatusIndicator(isStreaming, taskState?.statusText ?? null, taskState?.startTime ?? null);

    if (!taskState) {
        return null;
    }

    const steps = Array.from(taskState.thinkingSteps.values());

    // Если есть thinkingSteps, показываем их
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

    // Если шагов еще нет, но есть statusText, показываем его внутри общего контейнера
    if (statusIndicatorText) {
        return (
            <div className={styles.thinkingContainer}>
                <StatusIndicator status={statusIndicatorText} />
            </div>
        );
    }

    // Fallback по умолчанию (самое начало)
    return (
        <div className={styles.thinkingContainer}>
            <div className={styles.initialThinking}>
                <div className={styles.spinner}></div>
                <span>Анализирую ваш вопрос...</span>
            </div>
        </div>
    );
};
