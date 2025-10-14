import React, { FC } from 'react';
import { Shield, TrendingUp, Calendar, CheckCircle } from 'lucide-react';
import { TrustScoreReport } from '../types';
import styles from './TrustScoreIndicator.module.css';

/**
 * @interface TrustScoreIndicatorProps
 * @description Пропсы для компонента TrustScoreIndicator.
 */
interface TrustScoreIndicatorProps {
  /** @param {TrustScoreReport} report - Отчет об оценке доверия для отображения. */
  report: TrustScoreReport;
}

/**
 * Возвращает CSS-класс для цветового кодирования оценки.
 * @param {number} score - Оценка от 0 до 100.
 * @returns {string} Имя CSS-класса.
 */
const getScoreColorClass = (score: number): string => {
  if (score >= 85) return styles.highScore;
  if (score >= 60) return styles.mediumScore;
  return styles.lowScore;
};

/**
 * Компонент для визуализации "Оценки Доверия" (Trust Score) RAG-ответа.
 * Отображает итоговую оценку и предоставляет раскрывающийся блок с детализацией
 * по ключевым метрикам: уверенность AI, актуальность и авторитетность источников.
 * @param {TrustScoreIndicatorProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const TrustScoreIndicator: FC<TrustScoreIndicatorProps> = ({ report }) => {
  return (
    <details className={styles.details}>
      <summary className={styles.summary}>
        <div className={`${styles.scoreBadge} ${getScoreColorClass(report.finalScore)}`}>
          <Shield size={14} />
          <span>{report.finalScore} / 100</span>
        </div>
        <span className={styles.summaryTitle}>Оценка доверия</span>
      </summary>
      <div className={styles.breakdown}>
        <p className={styles.justification}>{report.justification}</p>
        <div className={styles.metricsGrid}>
          <div className={styles.metricItem}>
            <CheckCircle size={16} className={styles.metricIcon} />
            <div>
              <div className={styles.metricName}>Уверенность AI</div>
              <div className={styles.metricValue}>{report.confidenceScore}/100</div>
            </div>
          </div>
          <div className={styles.metricItem}>
            <Calendar size={16} className={styles.metricIcon} />
            <div>
              <div className={styles.metricName}>Актуальность</div>
              <div className={styles.metricValue}>{report.recencyScore}/100</div>
            </div>
          </div>
          <div className={styles.metricItem}>
            <TrendingUp size={16} className={styles.metricIcon} />
            <div>
              <div className={styles.metricName}>Авторитетность</div>
              <div className={styles.metricValue}>{report.authorityScore}/100</div>
            </div>
          </div>
        </div>
      </div>
    </details>
  );
};
