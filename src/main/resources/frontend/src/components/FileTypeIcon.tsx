import React from 'react';
import { FileText, FileCode2, FileJson, FileType, FileQuestion } from 'lucide-react';

/**
 * @interface FileTypeIconProps
 * @description Пропсы для компонента FileTypeIcon.
 */
interface FileTypeIconProps {
  /** @param {string} mimeType - MIME-тип файла. */
  mimeType: string;
}

/**
 * Атомарный компонент для отображения иконки файла в зависимости от его MIME-типа.
 * @param {FileTypeIconProps} props - Пропсы компонента.
 * @returns {React.ReactElement} Отрендеренная иконка.
 */
export const FileTypeIcon: React.FC<FileTypeIconProps> = ({ mimeType }) => {
  const getIcon = () => {
    if (mimeType.includes('java') || mimeType.includes('xml')) return <FileCode2 size={20} />;
    if (mimeType.includes('json')) return <FileJson size={20} />;
    if (mimeType.includes('markdown') || mimeType.includes('plain')) return <FileText size={20} />;
    if (mimeType.includes('pdf')) return <FileType size={20} />;
    return <FileQuestion size={20} />;
  };

  return <div style={{ color: 'hsl(var(--muted-foreground))' }}>{getIcon()}</div>;
};
