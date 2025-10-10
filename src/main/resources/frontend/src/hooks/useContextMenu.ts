import { useState, useCallback, useEffect, RefObject } from 'react';
import { useClickOutside } from './useClickOutside';

/**
 * @interface ContextMenuState<T>
 * @description Тип состояния для контекстного меню.
 * @template T - Тип элемента, к которому привязано меню.
 */
interface ContextMenuState<T> {
  /** @property {boolean} show - Флаг видимости меню. */
  show: boolean;
  /** @property {number} x - Координата X для позиционирования. */
  x: number;
  /** @property {number} y - Координата Y для позиционирования. */
  y: number;
  /** @property {T | null} item - Элемент, для которого было вызвано меню. */
  item: T | null;
}

/**
 * Хук для инкапсуляции логики управления всплывающим меню (контекстным или по клику).
 * @template T - Тип элемента, с которым ассоциировано меню.
 * @param {RefObject<HTMLDivElement>} menuRef - Ref-объект, указывающий на DOM-элемент самого меню.
 * @returns {{
 *   menuState: ContextMenuState<T>,
 *   openMenu: (event: React.MouseEvent, item: T) => void,
 *   closeMenu: () => void
 * }} Объект с состоянием меню и функциями для управления им.
 */
export function useContextMenu<T>(menuRef: RefObject<HTMLDivElement>) {
  const [menuState, setMenuState] = useState<ContextMenuState<T>>({ show: false, x: 0, y: 0, item: null });

  /**
   * Открывает меню, позиционируя его по координатам клика.
   * @param {React.MouseEvent} event - Событие мыши.
   * @param {T} item - Элемент, для которого открывается меню.
   */
  const openMenu = useCallback((event: React.MouseEvent, item: T) => {
    event.preventDefault();
    event.stopPropagation();
    setMenuState({ show: true, x: event.clientX, y: event.clientY, item });
  }, []);

  /**
   * Закрывает меню.
   */
  const closeMenu = useCallback(() => {
    setMenuState(prev => ({ ...prev, show: false }));
  }, []);

  // Закрытие меню по клику вне его области
  useClickOutside(menuRef, closeMenu);

  // Закрытие меню по нажатию Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        closeMenu();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [closeMenu]);

  return { menuState, openMenu, closeMenu };
}
