import { create } from 'zustand';

/**
 * @interface Attachment
 * @description Представляет один прикрепленный файл.
 * @deprecated Этот стор больше не используется. Заменен на useFileSelectionStore.
 */
interface Attachment {
  name: string;
  content: string;
}

/**
 * @interface AttachmentState
 * @description Определяет структуру состояния для управления прикрепленными файлами.
 * @deprecated Этот стор больше не используется.
 */
interface AttachmentState {
  attachments: Map<string, Attachment | null>;
  setAttachment: (sessionId: string, file: Attachment) => void;
  clearAttachment: (sessionId: string) => void;
}

/**
 * Глобальный стор Zustand для управления прикрепленными файлами в разных сессиях чата.
 * @deprecated Этот стор больше не используется. Вся логика работы с файлами
 * унифицирована через `useFileSelectionStore` и `useFiles`.
 */
export const useAttachmentStore = create<AttachmentState>((set) => ({
  attachments: new Map(),
  setAttachment: (sessionId, file) =>
    set((state) => ({
      attachments: new Map(state.attachments).set(sessionId, file),
    })),
  clearAttachment: (sessionId) =>
    set((state) => {
      const newAttachments = new Map(state.attachments);
      newAttachments.delete(sessionId);
      return { attachments: newAttachments };
    }),
}));
