import { useMemo } from 'react';
import { Message } from '../types';
import { useSessionStore } from '../state/sessionStore';

/**
 * @interface UseVisibleMessagesReturn
 * @description Определяет возвращаемое значение хука `useVisibleMessages`.
 */
interface UseVisibleMessagesReturn {
  /** @property {Message[]} visibleMessages - Отфильтрованный массив сообщений, которые должны быть видимы в UI. */
  visibleMessages: Message[];
  /** @property {Map<string, { total: number; current: number; siblings: Message[] }>} messageBranchInfo - Map с информацией о ветвлении для каждого сообщения ассистента. */
  messageBranchInfo: Map<string, { total: number; current: number; siblings: Message[] }>;
}

/**
 * @description Хук для инкапсуляции сложной логики определения видимых сообщений
 * с учетом ветвления ответов ассистента. Он является "селектором", который
 * зависит только от полного списка сообщений и глобального состояния UI (`useSessionStore`).
 *
 * @param {string} sessionId - ID текущей сессии.
 * @param {Message[]} messages - Полный массив всех сообщений в сессии.
 * @returns {UseVisibleMessagesReturn} Объект с отфильтрованными сообщениями и информацией о ветках.
 */
export const useVisibleMessages = (sessionId: string, messages: Message[]): UseVisibleMessagesReturn => {
  // Хук теперь зависит от глобального стора, а не от другого хука.
  const activeBranches = useSessionStore((state) => state.sessionBranches.get(sessionId) || {});

  const { visibleMessages, messageBranchInfo } = useMemo(() => {
    if (!messages.length) {
      return { visibleMessages: [], messageBranchInfo: new Map() };
    }
    // 1. Группируем все сообщения по их родителям.
    const childrenMap = new Map<string, Message[]>();
    messages.forEach(m => {
      if (m.parentId) {
        if (!childrenMap.has(m.parentId)) {
          childrenMap.set(m.parentId, []);
        }
        childrenMap.get(m.parentId)!.push(m);
      }
    });
    // Сортируем дочерние сообщения по дате создания для консистентности.
    childrenMap.forEach(children => children.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()));

    // 2. Определяем, какие сообщения нужно скрыть.
    const hiddenIds = new Set<string>();
    childrenMap.forEach((children, parentId) => {
      if (children.length > 1) {
        let selectedId = activeBranches[parentId];
        // Если активная ветка не выбрана или не существует, выбираем последнюю.
        if (!selectedId || !children.some(c => c.id === selectedId)) {
          selectedId = children[children.length - 1].id;
        }
        children.forEach(child => {
          if (child.id !== selectedId) {
            hiddenIds.add(child.id);
          }
        });
      }
    });

    // 3. Собираем мета-информацию о ветках для видимых сообщений.
    const branchInfo = new Map<string, { total: number; current: number; siblings: Message[] }>();
    messages.forEach(msg => {
      if (msg.type === 'assistant' && msg.parentId && !hiddenIds.has(msg.id)) {
        const siblings = childrenMap.get(msg.parentId) || [];
        if (siblings.length > 1) {
          const currentIndex = siblings.findIndex(s => s.id === msg.id);
          branchInfo.set(msg.id, { total: siblings.length, current: currentIndex + 1, siblings });
        }
      }
    });

    const visible = messages.filter(m => !hiddenIds.has(m.id));
    return { visibleMessages: visible, messageBranchInfo: branchInfo };
  }, [messages, activeBranches]);

  return { visibleMessages, messageBranchInfo };
};
