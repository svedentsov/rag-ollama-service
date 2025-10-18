import React, { FC } from 'react';
import { SourceCitation } from '../../../types';
import { Modal } from '../../../components/Modal';
import { CodeBlock } from './CodeBlock';

/**
 * @interface SourceViewerModalProps
 * @description Пропсы для модального окна просмотра источника.
 */
interface SourceViewerModalProps {
  /** @param {boolean} isOpen - Флаг, управляющий видимостью окна. */
  isOpen: boolean;
  /** @param {() => void} onClose - Колбэк для закрытия окна. */
  onClose: () => void;
  /** @param {SourceCitation | null} source - Объект источника для отображения. */
  source: SourceCitation | null;
}

/**
 * Модальное окно для просмотра полного содержимого документа-источника.
 * @param {SourceViewerModalProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренный компонент.
 */
export const SourceViewerModal: FC<SourceViewerModalProps> = ({ isOpen, onClose, source }) => {
  if (!source) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={source.sourceName}>
      <CodeBlock language="text">{source.textSnippet}</CodeBlock>
    </Modal>
  );
};
