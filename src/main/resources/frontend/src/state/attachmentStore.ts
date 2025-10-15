import { create } from 'zustand';

/**
 * @interface Attachment
 * @description Представляет один прикрепленный файл.
 */
interface Attachment {
  name: string;
  content: string;
}

/**
 * @interface AttachmentState
 * @description Определяет структуру состояния для управления прикрепленными файлами.
 */
interface AttachmentState {
  /** @property {Map<string, Attachment | null>} attachments - Карта, где ключ - ID сессии, значение - прикрепленный файл или null. */
  attachments: Map<string, Attachment | null>;
  /**
   * @function setAttachment
   * @description Прикрепляет файл к указанной сессии.
   * @param {string} sessionId - ID сессии.
   * @param {Attachment} file - Объект файла.
   */
  setAttachment: (sessionId: string, file: Attachment) => void;
  /**
   * @function clearAttachment
   * @description Удаляет прикрепленный файл из указанной сессии.
   * @param {string} sessionId - ID сессии.
   */
  clearAttachment: (sessionId: string) => void;
}

/**
 * Глобальный стор Zustand для управления прикрепленными файлами в разных сессиях чата.
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
