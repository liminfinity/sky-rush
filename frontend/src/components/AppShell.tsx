import { PlayerAvatar } from '../features/profile/Cosmetics';
import type { ReactNode } from 'react';
import type { Balance, Theme, User } from '../api/types';
import { amount } from '../utils/format';
import { SkyWorld } from './SkyWorld';
import { GameIcon } from './GameIcon';
interface Props {
  children: ReactNode;
  theme: Theme;
  user: User | null;
  balance: Balance | null;
  onProfile?: () => void;
  onHistory: () => void;
  onRules: () => void;
  tools?: ReactNode;
  mode?: string;
}
export function AppShell({
  children,
  theme,
  balance,
  user,
  onProfile,
  onHistory,
  onRules,
  tools,
  mode = '',
}: Props) {
  return (
    <div className={`app-shell theme-${theme.toLowerCase()} mode-${mode}`}>
      <SkyWorld theme={theme} />
      <a className="skip-link" href="#main">
        К игре
      </a>
      <header className="site-header">
        <div className="brand" aria-label="SkyRush">
          Sky<span>Rush</span>
        </div>
        <div className="wallet-header" title="Баланс">
          {user && (
            <button
              type="button"
              className="player-identity"
              aria-label="Профиль"
              title={user.displayName}
              onClick={onProfile}
            >
              <PlayerAvatar name={user.displayName} />
              <span>{user.displayName}</span>
            </button>
          )}
          <span className="bonus-coin" aria-hidden="true">
            Б
          </span>
          <strong data-testid="wallet-balance">
            {balance ? amount(balance.wallet.bonusBalance) : '…'}
          </strong>
          <span className="sr-only">бонусов</span>
        </div>
        <nav aria-label="Меню">
          <button
            type="button"
            className="hud-button"
            aria-label="История"
            title="История"
            onClick={onHistory}
          >
            <GameIcon name="history" />
          </button>
          <button
            type="button"
            className="hud-button"
            aria-label="Правила"
            title="Правила"
            onClick={onRules}
          >
            <GameIcon name="rules" />
          </button>
          {tools}
        </nav>
      </header>
      <main id="main">{children}</main>
    </div>
  );
}
