import { PlayerAvatar } from '../profile/Cosmetics';
import { useEffect, useRef } from 'react';
import type { Ranking } from '../../api/types';
import { integer } from '../../utils/format';
export function LiveRanking({ ranking }: { ranking?: Ranking }) {
  const list = useRef<HTMLOListElement>(null);
  const current = ranking?.entries.find((e) => e.currentPlayer);
  useEffect(() => {
    const element = list.current?.querySelector<HTMLElement>(
      '[aria-current="true"]',
    );
    const parent = list.current;
    if (!element || !parent) return;
    const left = element.offsetLeft - parent.offsetLeft;
    if (
      left < parent.scrollLeft ||
      left + element.offsetWidth > parent.scrollLeft + parent.clientWidth
    )
      parent.scrollLeft = Math.max(
        0,
        left - parent.clientWidth / 2 + element.offsetWidth / 2,
      );
  }, [current?.position]);
  if (!ranking) return null;
  return (
    <section className="live-ranking" aria-label="Рейтинг полёта">
      <div className="ranking-caption">
        <small title="Очки за сегодня">Рейтинг</small>
      </div>
      <ol ref={list}>
        {ranking.entries.map((e) => (
          <li
            key={e.id}
            className={e.currentPlayer ? 'current-player' : ''}
            aria-current={e.currentPlayer ? 'true' : undefined}
          >
            <span>
              {e.currentPlayer && <PlayerAvatar name={e.name} />}#{e.position}{' '}
              {e.name}
              {e.currentPlayer ? ' (Ты)' : ''}
            </span>
            <strong
              key={e.points}
              className={e.currentPlayer ? 'score-flash' : ''}
            >
              {integer(e.points)} очк.
            </strong>
          </li>
        ))}
      </ol>
    </section>
  );
}
