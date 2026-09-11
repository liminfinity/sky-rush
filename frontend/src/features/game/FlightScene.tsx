import type { CSSProperties } from 'react';
import { isCompleted, type Round } from '../../api/types';
import { Balloon } from '../../components/Balloon';
import { multiplier } from '../../utils/format';
export function FlightScene({ round }: { round: Round }) {
  const done = isCompleted(round);
  // Server level controls presentation position/tension only; no client multiplier or timing model.
  const progress = round.completedLevel / round.levelThresholds.length;
  return (
    <section
      className={`flight-scene ${done ? 'crashed' : ''} ${round.booster.active ? 'boosted' : ''} tension-${round.completedLevel < 2 ? 'calm' : progress < 0.5 ? 'warm' : 'high'}`}
      aria-label="Полёт воздушного шара"
    >
      <div className="multiplier-focus">
        <span>{done ? 'Лопнул на' : ''}</span>
        <strong data-testid="live-multiplier">
          {multiplier(done ? round.crashMultiplier : round.currentMultiplier)}
        </strong>
        <small>
          {round.completedLevel} / {round.levelThresholds.length} уровней
        </small>
      </div>
      <ol className="level-ladder" aria-label="Уровни полёта">
        {round.levelThresholds.map((threshold, index) => {
          const crossed = index < round.completedLevel,
            boosterHere = round.booster.level === index + 1;
          return (
            <li
              key={index}
              className={`${crossed ? 'crossed' : ''} ${index === round.completedLevel ? 'next' : ''}`}
              aria-label={`Уровень ${index + 1}${crossed ? ', пройден' : ', впереди'}${boosterHere ? ', бустер' : ''}`}
            >
              <span className="level-number">
                {crossed ? '✓' : String(index + 1).padStart(2, '0')}
              </span>
              <span className="level-line" />
              <span className="level-threshold">{multiplier(threshold)}</span>
              {boosterHere && (
                <span
                  className={`level-booster ${round.booster.active ? 'activated' : round.cashoutAt || done ? 'missed' : ''}`}
                  aria-label={`Бустер ×${round.booster.multiplier}, уровень ${index + 1}`}
                >
                  <b>×{round.booster.multiplier}</b>
                </span>
              )}
            </li>
          );
        })}
      </ol>
      <div
        className="flight-balloon-position"
        style={{ '--flight-rise': `${progress * 180}px` } as CSSProperties}
      >
        <div
          className="flight-balloon"
          key={round.booster.active ? 'boosted' : 'normal'}
        >
          <Balloon />
        </div>
        {done && (
          <div className="burst" aria-hidden="true">
            {Array.from({ length: 9 }, (_, i) => (
              <i key={i} style={{ '--particle': i } as CSSProperties} />
            ))}
          </div>
        )}
      </div>
      {done && (
        <div className="crash-caption" role="status">
          <strong>Шар лопнул</strong>
          <span>
            {round.state === 'COMPLETED_WIN'
              ? 'Выигрыш сохранён'
              : 'Ставка сгорела'}
          </span>
        </div>
      )}
    </section>
  );
}
