import React from 'react';
import { ArrowDownCircle } from 'lucide-react';
import styles from './ScrollToBottomButton.module.css';

interface ScrollToBottomButtonProps {
    onClick: () => void;
}

export const ScrollToBottomButton: React.FC<ScrollToBottomButtonProps> = ({ onClick }) => {
    return (
        <button className={styles.scrollButton} onClick={onClick} title="Перейти к последнему сообщению">
            <ArrowDownCircle size={20} />
        </button>
    );
};
