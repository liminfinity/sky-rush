import { PlayerAvatar } from '../profile/Cosmetics';
import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import type { Tournament, RankingEntry } from '../../api/types';
import { Notice } from '../../components/Notice';
import { integer } from '../../utils/format';
function Row({ entry }: { entry: RankingEntry }) {
  return (
    <li className={entry.currentPlayer ? 'current-player' : ''}>
      <span>
        {entry.currentPlayer && <PlayerAvatar name={entry.name} />}#
        {entry.position} {entry.name}
        {entry.currentPlayer ? ' (Ты)' : ''}
      </span>
      <b>{integer(entry.points)} очк.</b>
    </li>
  );
}
export function TournamentTable() {
  const [data, setData] = useState<Tournament | null>(null),
    [error, setError] = useState(''),
    [masked, setMasked] = useState(true);
  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    const controller = new AbortController();
    const poll = async () => {
      try {
        const value = await api.tournament(masked, controller.signal);
        if (!disposed) {
          setData(value);
          setError('');
        }
      } catch (e) {
        if (!disposed) setError(errorMessage(e));
      } finally {
        if (!disposed) timer = setTimeout(() => void poll(), 900);
      }
    };
    void poll();
    return () => {
      disposed = true;
      controller.abort();
      clearTimeout(timer);
    };
  }, [masked]);
  // A podium needs earned points. Never fill missing places or duplicate the current player.
  const podium = data?.entries.filter((e) => e.points > 0).slice(0, 3) ?? [];
  const rest =
    data?.entries.filter(
      (e) => !e.currentPlayer && !podium.some((p) => p.id === e.id),
    ) ?? [];
  const pinned = data && !podium.some((e) => e.currentPlayer);
  return (
    <section className="tournament">
      <label>
        <input
          type="checkbox"
          checked={masked}
          onChange={(e) => setMasked(e.target.checked)}
        />{' '}
        Скрывать имена
      </label>
      {error && <Notice subtle>{error}</Notice>}
      {!data ? (
        <p role="status">Загружаем…</p>
      ) : (
        <>
          <p>
            {data.status === 'ACTIVE' ? 'До конца' : 'Завершён'}
            {data.status === 'ACTIVE' &&
              ` ${Math.floor(data.remainingSeconds / 60)} мин. ${data.remainingSeconds % 60} сек.`}
          </p>
          {podium.length > 0 && (
            <ol className="podium" aria-label="Лидеры">
              {podium.map((e) => (
                <Row key={e.id} entry={e} />
              ))}
            </ol>
          )}
          {rest.length > 0 && (
            <ol className="tournament-rest" aria-label="Игроки">
              {rest.map((e) => (
                <Row key={e.id} entry={e} />
              ))}
            </ol>
          )}
          {pinned && (
            <ol className="pinned-player" aria-label="Твоё место">
              <Row entry={data.currentPlayer} />
            </ol>
          )}
        </>
      )}
    </section>
  );
}
