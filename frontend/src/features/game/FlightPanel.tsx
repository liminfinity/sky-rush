import { useEffect, useRef, useState } from 'react';
import { isCompleted, type Round } from '../../api/types';
import { amount, integer, multiplier } from '../../utils/format';
import { BoosterStatus } from './BoosterStatus';
export interface FlightPanelProps {
  round: Round;
  cashout: () => void;
  busy: boolean;
  reconnecting: boolean;
}
export function FlightPanel({
  round,
  cashout,
  busy,
  reconnecting,
}: FlightPanelProps) {
  const previous = useRef(round.earnedPoints);
  const [delta, setDelta] = useState(0);
  const [hint, setHint] = useState(false);
  useEffect(() => {
    const difference = round.earnedPoints - previous.current;
    previous.current = round.earnedPoints;
    if (difference <= 0) return;
    setDelta(difference);
    const t = setTimeout(() => setDelta(0), 1100);
    return () => clearTimeout(t);
  }, [round.earnedPoints]);
  const done = isCompleted(round),
    fixed = round.cashoutAt !== null;
  useEffect(() => {
    if (!round.canCashout || fixed || done) {
      setHint(false);
      return;
    }
    try {
      if (sessionStorage.getItem('skyrush.cashout-hint')) return;
      sessionStorage.setItem('skyrush.cashout-hint', 'seen');
    } catch {
      /* Hint remains optional when storage is blocked. */
    }
    setHint(true);
  }, [round.canCashout, fixed, done]);
  useEffect(() => {
    if (!hint) return;
    const t = setTimeout(() => setHint(false), 4000);
    return () => clearTimeout(t);
  }, [hint]);
  return (
    <section className="flight-controls" aria-label="Управление полётом">
      <div className="round-stats">
        <div>
          <span>Ставка</span>
          <strong>{amount(round.stake)} Б</strong>
        </div>
        <div>
          <span>Очки</span>
          <strong data-testid="round-points">
            {integer(round.earnedPoints)}
          </strong>
          {delta > 0 && (
            <span
              key={round.earnedPoints}
              className="points-delta"
              aria-label={`+${delta} очков`}
              aria-live="polite"
            >
              +{delta}
            </span>
          )}
        </div>
      </div>
      <div className="cashout-dock">
        <BoosterStatus round={round} />
        {fixed && (
          <div className="fixed-win" role="status">
            <span>Забрали {multiplier(round.cashoutMultiplier)}</span>
            <strong data-testid="fixed-payout">{amount(round.payout)} Б</strong>
            <p>
              {done ? 'Выигрыш сохранён' : 'Шар летит дальше…'}{' '}
              <small>Могли бы забрать больше</small>
            </p>
          </div>
        )}
        <div className="cashout-bar">
          {hint && !busy && (
            <div className="cashout-hint">
              Нажми «Забрать», пока шар не лопнул{' '}
              <span aria-hidden="true">↓</span>
            </div>
          )}
          <button
            type="button"
            className="primary-button cashout-button"
            onClick={cashout}
            disabled={!round.canCashout || busy || reconnecting || done}
          >
            {busy
              ? 'Забираем…'
              : fixed
                ? 'Забрали'
                : done
                  ? 'Шар лопнул'
                  : 'Забрать'}
          </button>
          {((!round.canCashout && !fixed && !done) || reconnecting) && (
            <p className="small-note">
              {reconnecting ? 'Соединяемся…' : 'После первого уровня'}
            </p>
          )}
        </div>
      </div>
    </section>
  );
}
