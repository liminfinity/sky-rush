import { FragmentProgress } from '../rewards/FragmentProgress';
import type { Balance, PublicConfig, Theme } from '../../api/types';
import { Balloon } from '../../components/Balloon';
import { amount, multiplier, themeName } from '../../utils/format';

interface Props {
  config: PublicConfig;
  balance: Balance | null;
  theme: Theme;
  selected: string | null;
  chooseTheme: (theme: Theme) => void;
  select: (id: string) => void;
  start: () => void;
  starting: boolean;
  pending: boolean;
  unavailable: boolean;
}
export function BetSelection({
  config,
  balance,
  theme,
  selected,
  chooseTheme,
  select,
  start,
  starting,
  pending,
  unavailable,
}: Props) {
  const bet = config.betOptions.find((value) => value.id === selected);
  const affordable = (stake: number) =>
    balance !== null && stake <= balance.wallet.bonusBalance;
  const levels = config.themes[theme];
  return (
    <div className="bet-layout">
      <section className="selection-sky" aria-label={themeName(theme)}>
        <div className="preview-balloon">
          <Balloon />
        </div>
        <div
          className="preview-levels"
          aria-label={`${levels.levels} уровней полёта`}
        >
          {levels.levelThresholds.map((value, i) => (
            <span key={i} title={`Уровень ${i + 1}: ${multiplier(value)}`}>
              {i + 1}
            </span>
          ))}
        </div>
      </section>
      <section className="bet-panel" aria-labelledby="bet-title">
        <h1 id="bet-title">Выбери ставку</h1>
        <div className="theme-switch" role="group" aria-label="Тема полёта">
          {(['GREEN', 'RED'] as const).map((value) => (
            <button
              type="button"
              key={value}
              aria-label={`${value} ${config.themes[value].levels} уровней`}
              aria-pressed={theme === value}
              disabled={starting || pending}
              onClick={() => chooseTheme(value)}
            >
              <i className={`theme-dot ${value.toLowerCase()}`} />
              {value === 'GREEN' ? 'Зелёный' : 'Красный'}
              <small>{config.themes[value].levels} ур.</small>
            </button>
          ))}
        </div>
        <div className="bet-options">
          {config.betOptions.map((option) => (
            <button
              type="button"
              key={option.id}
              className={`bet-card ${selected === option.id ? 'selected' : ''}`}
              aria-pressed={selected === option.id}
              disabled={!affordable(option.stake) || starting || pending}
              aria-label={`Ставка ${option.stake}, бустер x${option.boosterMultiplier}${!affordable(option.stake) ? ', недостаточно бонусов' : ''}`}
              onClick={() => select(option.id)}
            >
              <strong>
                {amount(option.stake)} <small>Б</small>
              </strong>
              <span
                className="bet-booster"
                title={
                  option.boosterMultiplier === 1
                    ? 'Без усиления'
                    : 'Достигни бустера до «Забрать»'
                }
              >
                ×{option.boosterMultiplier}
              </span>
              {!affordable(option.stake) && (
                <small className="unaffordable">Не хватает бонусов</small>
              )}
            </button>
          ))}
        </div>
        <button
          type="button"
          className="primary-button start-button"
          disabled={
            starting ||
            (!pending && (!bet || !affordable(bet.stake) || unavailable))
          }
          onClick={start}
        >
          {starting ? 'Взлетаем…' : pending ? 'Проверить' : 'Начать'}
        </button>
        <FragmentProgress balance={balance} rules={config.reward} />
      </section>
    </div>
  );
}
