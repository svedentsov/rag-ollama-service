import React, { FC } from 'react';
import { useStreamingStore } from '../../../state/streamingStore';
import { ThinkingStep } from './ThinkingStep';
import { StatusIndicator } from './StatusIndicator';
import styles from './ThinkingThoughts.module.css';

interface ThinkingThoughtsProps {
  assistantMessageId: string;
}

export const ThinkingThoughts: FC<ThinkingThoughtsProps> = ({ assistantMessageId }) => {
    const taskState = useStreamingStore((state) => state.activeStreams.get(assistantMessageId));

    if (!taskState) return null;

    const steps = Array.from(taskState.thinkingSteps.values());

    if (steps.length > 0) {
        return (
            <div className={styles.thinkingContainer}>
                <div className={styles.header}>
                    <div className={styles.spinner}></div>
                    <span>Выполняю план...</span>
                </div>
                <div className={styles.stepsList}>
                    {steps.map((step) => <ThinkingStep key={step.name} step={step} />)}
                </div>
            </div>
        );
    }

    if (taskState.statusText) {
        return (
            <div className={styles.thinkingContainer}>
                <StatusIndicator status={taskState.statusText} startTime={taskState.startTime} />
            </div>
        );
    }

    return (
        <div className={styles.thinkingContainer}>
            <div className={styles.initialThinking}>
                <div className={styles.spinner}></div>
                <span>Анализирую ваш вопрос...</span>
            </div>
        </div>
    );
};
