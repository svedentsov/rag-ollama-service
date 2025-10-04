import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { api } from '../api';

/**
 * Хук для отправки обратной связи (оценки) по ответу ассистента.
 */
export function useFeedback() {
  return useMutation({
    mutationFn: api.sendFeedback,
    onSuccess: () => {
      toast.success('Спасибо за ваш отзыв!');
    },
    onError: (error: Error) => {
      toast.error(`Ошибка при отправке отзыва: ${error.message}`);
    },
  });
}
