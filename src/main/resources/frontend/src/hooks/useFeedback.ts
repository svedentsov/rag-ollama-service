import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import * as api from '../api';

/**
 * Хук для отправки обратной связи (оценки) по ответу ассистента.
 * Инкапсулирует вызов API и обработку состояний загрузки/ошибки.
 */
export function useFeedback() {
  return useMutation({
    mutationFn: ({ taskId, isHelpful }: { taskId: string; isHelpful: boolean }) =>
      api.sendFeedback({ taskId, isHelpful }),
    onSuccess: () => {
      toast.success('Спасибо за ваш отзыв!');
    },
    onError: (error: Error) => {
      toast.error(`Ошибка при отправке отзыва: ${error.message}`);
    },
  });
}
