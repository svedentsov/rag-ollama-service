import React, { FC } from 'react';
import { Search, X } from 'lucide-react';
import styles from './SearchInput.module.css';

/**
 * @interface SearchInputProps
 * @description Пропсы для компонента SearchInput.
 */
interface SearchInputProps {
  /** @param {string} value - Текущее значение поля ввода. */
  value: string;
  /** @param {(newValue: string) => void} onChange - Колбэк, вызываемый при изменении значения. */
  onChange: (newValue: string) => void;
  /** @param {string} placeholder - Текст плейсхолдера. */
  placeholder?: string;
  /** @param {string} ariaLabel - Aria-label для доступности. */
  ariaLabel?: string;
}

/**
 * Переиспользуемый компонент поля поиска с иконками и кнопкой очистки.
 * @param {SearchInputProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const SearchInput: FC<SearchInputProps> = ({
  value,
  onChange,
  placeholder = "Поиск...",
  ariaLabel = "Поле поиска"
}) => {
  return (
    <div className={styles.searchWrapper}>
      <Search className={styles.searchIcon} size={16} aria-hidden="true" />
      <input
        type="text"
        placeholder={placeholder}
        className={styles.searchInput}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={ariaLabel}
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          className={styles.clearButton}
          aria-label="Очистить поиск"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
};
