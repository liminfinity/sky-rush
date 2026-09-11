import { AchievementArt } from './AchievementArt';
import type {
  Achievement,
  DailyChallenge,
  SocialActivity,
} from '../../api/types';
import { dateTime, multiplier } from '../../utils/format';
export function DailyCard({ daily }: { daily: DailyChallenge | null }) {
  if (!daily) return null;
  return (
    <aside
      className="daily-card"
      aria-label="Сегодня"
      title="Новое задание в 00:00 UTC"
    >
      <div>
        <strong>Сегодня</strong>
        <span>{daily.completed ? 'Готово' : daily.description}</span>
      </div>
      <div>
        <b>
          {daily.completed
            ? '+1 фрагмент'
            : `${daily.progress.toLocaleString('ru-RU', { maximumFractionDigits: 4 })} / ${daily.target.toLocaleString('ru-RU')}`}
        </b>
        {!daily.completed && <small>+1 фрагмент</small>}
      </div>
    </aside>
  );
}
export function AchievementList({ items }: { items: Achievement[] }) {
  return (
    <details className="achievements">
      <summary>
        Достижения {items.filter((i) => i.unlockedAt).length} / {items.length}
      </summary>
      <ul>
        {items.map((item) => (
          <li key={item.id} className={item.unlockedAt ? 'earned' : 'locked'}>
            <AchievementArt id={item.id} />
            <div>
              <strong>{item.name}</strong>
              <small>{item.description}</small>
              {item.unlockedAt && (
                <time dateTime={item.unlockedAt}>
                  {dateTime(item.unlockedAt)}
                </time>
              )}
            </div>
          </li>
        ))}
      </ul>
    </details>
  );
}
export function AchievementReveal({
  items,
  roundId,
  daily,
}: {
  items: Achievement[];
  roundId: string;
  daily: DailyChallenge | null;
}) {
  const earned = items.filter((i) => i.roundId === roundId);
  return (
    <>
      {earned.length > 0 && (
        <aside className="achievement-reveal" role="status">
          <strong>Достижение открыто</strong>
          <div className="achievement-reveal-items">
            {earned.map((i) => (
              <span key={i.id}>
                <AchievementArt id={i.id} />
                <b>{i.name}</b>
              </span>
            ))}
          </div>
        </aside>
      )}
      {daily?.rewardRoundId === roundId && (
        <p className="daily-reveal">Задание выполнено: +1 фрагмент</p>
      )}
    </>
  );
}
export function SocialFeed({
  data,
  error,
}: {
  data: SocialActivity | null;
  error?: string;
}) {
  return (
    <aside className="social-feed" aria-label="Сейчас в игре">
      <details open>
        <summary>
          <strong>Сейчас в игре</strong>
          <span className="online-count" title="Недавно были в игре">
            <i aria-hidden="true" />
            Онлайн: {data?.online ?? '…'}
          </span>
        </summary>
        {error && <small role="status">Соединяемся…</small>}
        {data &&
          (data.events.length ? (
            <ul>
              {data.events.slice(0, 5).map((e) => (
                <li key={e.id}>
                  <span
                    className={`activity-picture ${e.kind.toLowerCase()}`}
                    aria-hidden="true"
                  >
                    {e.kind === 'ACHIEVEMENT' ? (
                      <AchievementArt id="FIRST_WIN" />
                    ) : e.kind === 'BOOSTER' ? (
                      <AchievementArt id="BOOSTER_HUNTER" />
                    ) : (
                      <svg viewBox="0 0 32 32" fill="none">
                        <path
                          d="M8 5h16v11a8 8 0 0 1-16 0V5ZM8 8H3v6a6 6 0 0 0 7 6m14-12h5v6a6 6 0 0 1-7 6m-6 4v5m-5 0h10"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                        />
                      </svg>
                    )}
                  </span>
                  <div>
                    <b>{e.displayName}</b>
                    <span>
                      {e.kind === 'CASHOUT'
                        ? `забрал ${multiplier(Number(e.value))}`
                        : e.kind === 'BOOSTER'
                          ? `бустер ×${e.value}`
                          : `Открыто «${e.value}»`}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p>Пока тихо</p>
          ))}
      </details>
    </aside>
  );
}
