/**
 * Форматирует размер файла из байт в человекочитаемый формат (KB, MB, GB).
 * @param {number} bytes - Размер файла в байтах.
 * @returns {string} Отформатированная строка.
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

/**
 * Безопасно форматирует строку с датой в локализованный, человекочитаемый вид.
 * Использует `Intl.DateTimeFormat` для надежного и консистентного отображения.
 * @param {string | undefined} dateString - Строка с датой в формате ISO 8601.
 * @returns {string} Отформатированная строка с датой и временем или пустая строка в случае ошибки.
 */
export const formatDate = (dateString: string | undefined): string => {
  if (!dateString) {
    return '';
  }
  try {
    const date = new Date(dateString);
    // Проверяем, является ли дата валидной
    if (isNaN(date.getTime())) {
      return 'Неверная дата';
    }
    return new Intl.DateTimeFormat('ru-RU', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  } catch (error) {
    console.error('Ошибка форматирования даты:', dateString, error);
    return 'Неверная дата';
  }
};
