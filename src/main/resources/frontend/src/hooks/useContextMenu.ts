import { useState, useCallback, useEffect, RefObject } from 'react';
import { useClickOutside } from './useClickOutside';

/**
 * Тип состояния для контекстного меню.
 * @template T - Тип элемента, к которому привязано меню.
 */
interface ContextMenuState<T> {
  /** Флаг видимости меню. */
  show: boolean;
  /** Координата X для позиционирования. */
  x: number;
  /** Координата Y для позиционирования. */
  y: number;
  /** Элемент, для которого было вызвано меню. */
  item: T | null;
}

/**
 * Хук для инкапсуляции логики управления контекстным меню.
 * @template T - Тип элемента, с которым ассоциировано меню.
 * @param menuRef - Ref-объект, указывающий на DOM-элемент самого меню.
 * @returns Объект с состоянием меню и функциями для управления им.
 */
export function useContextMenu<T>(menuRef: RefObject<HTMLDivElement>) {
  const [menuState, setMenuState] = useState<ContextMenuState<T>>({ show: false, x: 0, y: 0, item: null });

  const handleContextMenu = useCallback((event: React.MouseEvent, item: T) => {
    event.preventDefault();
    event.stopPropagation();
    setMenuState({ show: true, x: event.clientX, y: event.clientY, item });
  }, []);

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

  return { menuState, handleContextMenu, closeMenu };
}
