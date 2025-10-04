import { useMemo } from 'react';
import { Message } from '../types';
import { useChatSessions } from './useChatSessions';

/**
 * Результат работы хука useVisibleMessages.
 */
interface UseVisibleMessagesReturn {
  /** Отфильтрованный массив сообщений, которые должны быть видимы в UI. */
  visibleMessages: Message[];
  /** Map с информацией о ветвлении для каждого сообщения ассистента. */
  messageBranchInfo: Map<string, { total: number; current: number; siblings: Message[] }>;
}

/**
 * Хук для инкапсуляции сложной логики определения видимых сообщений
 * с учетом ветвления ответов ассистента.
 * @param {string} sessionId - ID текущей сессии.
 * @param {Message[]} messages - Полный массив всех сообщений в сессии.
 * @returns {UseVisibleMessagesReturn} Объект с отфильтрованными сообщениями и информацией о ветках.
 */
export const useVisibleMessages = (sessionId: string, messages: Message[]): UseVisibleMessagesReturn => {
  const { sessions } = useChatSessions();
  const activeBranches = useMemo(() => {
      const currentSession = sessions.find(s => s.sessionId === sessionId);
      return currentSession?.activeBranches ?? {};
  }, [sessions, sessionId]);

  const { visibleMessages, messageBranchInfo } = useMemo(() => {
    if (!messages.length) {
      return { visibleMessages: [], messageBranchInfo: new Map() };
    }

    const childrenMap = new Map<string, Message[]>();
    messages.forEach(m => {
      if (m.parentId) {
        if (!childrenMap.has(m.parentId)) {
          childrenMap.set(m.parentId, []);
        }
        childrenMap.get(m.parentId)!.push(m);
      }
    });

    const hiddenIds = new Set<string>();
    childrenMap.forEach((children, parentId) => {
      if (children.length > 1) {
        let selectedId = activeBranches[parentId];
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

    const branchInfo = new Map<string, { total: number; current: number; siblings: Message[] }>();
    messages.forEach(msg => {
      if (msg.type === 'assistant' && msg.parentId) {
        const siblings = childrenMap.get(msg.parentId) || [];
        if (siblings.length > 1) {
          const activeId = activeBranches[msg.parentId] || siblings[siblings.length - 1].id;
          const currentIndex = siblings.findIndex(s => s.id === activeId);
          branchInfo.set(msg.id, { total: siblings.length, current: currentIndex + 1, siblings });
        }
      }
    });

    const visible = messages.filter(m => !hiddenIds.has(m.id));
    return { visibleMessages: visible, messageBranchInfo: branchInfo };
  }, [messages, activeBranches]);

  return { visibleMessages, messageBranchInfo };
};
