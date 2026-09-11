import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import type { History } from '../../api/types';
import { amount, dateTime, integer, multiplier } from '../../utils/format';
import { Notice } from '../../components/Notice';
export function HistoryList({ revision }: { revision: number }) {
  const [data, setData] = useState<History | null>(null);
  const [offset, setOffset] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    api
      .history(offset, controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setData(value);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [offset, revision, retry]);
  return (
    <div className="history-content">
      {loading && <p role="status">Загружаем…</p>}
      {error && <Notice retry={() => setRetry((v) => v + 1)}>{error}</Notice>}
      {!loading && !error && data?.rounds.length === 0 && (
        <div className="empty-state">
          <span aria-hidden="true">↗</span>
          <h3>{offset ? 'Больше полётов нет' : 'Полётов пока нет'}</h3>
        </div>
      )}
      {!loading &&
        !error &&
        data?.rounds.map((round) => (
          <article className="history-round" key={round.id}>
            <div className="history-title">
              <span
                className={`result-badge ${round.state === 'COMPLETED_WIN' ? 'win' : 'loss'}`}
              >
                {round.state === 'COMPLETED_WIN' ? 'Победа' : 'Проигрыш'}
              </span>
              <b>{round.theme === 'RED' ? 'Красный' : 'Зелёный'}</b>
              <time dateTime={round.completedAt!}>
                {dateTime(round.completedAt!)}
              </time>
            </div>
            <dl>
              <div>
                <dt>Ставка</dt>
                <dd>{amount(round.stake)} Б</dd>
              </div>
              <div>
                <dt>Выигрыш</dt>
                <dd>{amount(round.payout)} Б</dd>
              </div>
              <div>
                <dt>Забрали</dt>
                <dd>{multiplier(round.cashoutMultiplier)}</dd>
              </div>
              <div>
                <dt>Взрыв</dt>
                <dd>{multiplier(round.crashMultiplier)}</dd>
              </div>
              <div>
                <dt>Очки</dt>
                <dd>{integer(round.earnedPoints)}</dd>
              </div>
              <div>
                <dt>Награда</dt>
                <dd>+{round.reward?.fragmentsGranted ?? 0} ✦</dd>
              </div>
            </dl>
            <p className="history-booster">
              Бустер ×{round.booster.multiplier}{' '}
              {round.booster.multiplier === 1
                ? 'без усиления'
                : round.booster.active
                  ? 'активирован'
                  : 'упущен'}
            </p>
            {round.reward && round.reward.fragmentsRedeemed > 0 && (
              <p className="history-booster">
                Обмен {round.reward.fragmentsRedeemed} ✦ за +
                {amount(round.reward.bonusBalanceGranted)} Б
              </p>
            )}
            <details className="round-id">
              <summary>Номер полёта</summary>
              {round.id}
            </details>
          </article>
        ))}
      <div className="pagination">
        <button
          type="button"
          className="quiet-button"
          disabled={offset === 0 || loading}
          onClick={() => setOffset((v) => Math.max(0, v - 10))}
        >
          Новее
        </button>
        <span>Страница {offset / 10 + 1}</span>
        <button
          type="button"
          className="quiet-button"
          disabled={loading || !!error || !data || data.rounds.length < 10}
          onClick={() => setOffset((v) => v + 10)}
        >
          Раньше
        </button>
      </div>
    </div>
  );
}
