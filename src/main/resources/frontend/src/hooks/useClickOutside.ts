import { useEffect, RefObject } from 'react';

type Event = MouseEvent | TouchEvent;

/**
 * Кастомный хук, который отслеживает клики за пределами указанного элемента.
 * @param {RefObject<HTMLElement>} ref - Ref-объект, указывающий на DOM-элемент, для которого нужно отслеживать клики снаружи.
 * @param {() => void} handler - Функция, которая будет вызвана при клике за пределами элемента.
 */
export function useClickOutside(ref: RefObject<HTMLElement>, handler: () => void) {
  useEffect(() => {
    const listener = (event: Event) => {
      // Ничего не делаем, если ref не существует или клик был внутри самого элемента
      if (!ref.current || ref.current.contains(event.target as Node)) {
        return;
      }
      handler();
    };

    document.addEventListener('mousedown', listener);
    document.addEventListener('touchstart', listener);

    return () => {
      document.removeEventListener('mousedown', listener);
      document.removeEventListener('touchstart', listener);
    };
  }, [ref, handler]); // Перезапускаем эффект только если ref или handler изменились
}
