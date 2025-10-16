import React from 'react';
import { MessageSquare, Files } from 'lucide-react';
import { useRouter } from '../hooks/useRouter';
import styles from './RootSidebar.module.css';

/**
 * @interface NavItemProps
 * @description Пропсы для одного элемента навигации.
 */
interface NavItemProps {
  /** @param {string} href - Целевой URL для навигации. */
  href: string;
  /** @param {string} label - Текстовая метка (для tooltip). */
  label: string;
  /** @param {React.ReactNode} icon - Иконка элемента. */
  icon: React.ReactNode;
  /** @param {boolean} isActive - Флаг, является ли этот элемент активным. */
  isActive: boolean;
}

/**
 * Презентационный компонент для одного элемента в боковой панели.
 * @param {NavItemProps} props - Пропсы компонента.
 * @returns {React.ReactElement}
 */
const NavItem: React.FC<NavItemProps> = ({ href, label, icon, isActive }) => {
  const { navigate } = useRouter();

  return (
    <a
      href={href}
      className={`${styles.navItem} ${isActive ? styles.active : ''}`}
      onClick={(e) => {
        e.preventDefault();
        navigate(href);
      }}
      title={label}
      aria-label={label}
      aria-current={isActive ? 'page' : undefined}
    >
      {icon}
    </a>
  );
};

/**
 * Корневая боковая панель для навигации между основными разделами приложения.
 * @returns {React.ReactElement}
 */
export const RootSidebar: React.FC = () => {
    const { pathname } = useRouter();

    return (
        <aside className={styles.rootSidebar}>
            <nav className={styles.nav}>
                <NavItem
                    href="/chat"
                    label="AI Чат"
                    icon={<MessageSquare size={24} />}
                    isActive={pathname.startsWith('/chat') || pathname === '/'}
                />
                <NavItem
                    href="/files"
                    label="Файловый менеджер"
                    icon={<Files size={24} />}
                    isActive={pathname.startsWith('/files')}
                />
            </nav>
        </aside>
    );
};
