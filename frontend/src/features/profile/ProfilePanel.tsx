import { DailyCard, AchievementList } from '../social/SocialPanels';
import type { Achievement, DailyChallenge } from '../../api/types';
import type { Collection, CosmeticItem, Profile } from '../../api/types';
import { Balloon } from '../../components/Balloon';
import { Notice } from '../../components/Notice';
import { amount, integer, multiplier } from '../../utils/format';
import { PlayerAvatar } from './Cosmetics';
export function CosmeticPreview({ item }: { item: CosmeticItem }) {
  return (
    <span className="cosmetic-preview">
      {item.kind === 'BALLOON' ? (
        <Balloon small skin={item.id} />
      ) : (
        <PlayerAvatar name="S" frame={item.id} />
      )}
    </span>
  );
}
export function CollectionShelf({
  collection,
  equip,
  busy,
}: {
  collection: Collection;
  equip: (id: string) => void;
  busy: string | null;
}) {
  return (
    <section className="collection-shelf">
      <h3>Коллекция</h3>
      <p className="collection-progress">
        <strong>
          {integer(collection.lifetimeFragments)}
          {collection.nextItemName
            ? ` / ${integer(collection.nextThreshold)}`
            : ''}{' '}
          фрагментов
        </strong>
        <span>
          {collection.nextItemName
            ? `Ещё ${integer(collection.fragmentsToNext)} до «${collection.nextItemName}»`
            : 'Всё открыто'}
        </span>
      </p>
      <ul>
        {collection.items.map((item) => (
          <li key={item.id} className={item.unlocked ? 'unlocked' : 'locked'}>
            <CosmeticPreview item={item} />
            <strong>{item.name}</strong>
            <small>{item.kind === 'BALLOON' ? 'Узор шара' : 'Рамка'}</small>
            <button
              type="button"
              className="quiet-button"
              disabled={!item.unlocked || item.equipped || busy !== null}
              onClick={() => equip(item.id)}
              aria-label={`${item.equipped ? 'Выбрано' : item.unlocked ? 'Надеть' : 'Закрыто'}: ${item.name}`}
            >
              {busy === item.id
                ? 'Надеваем…'
                : item.equipped
                  ? 'Выбрано'
                  : item.unlocked
                    ? 'Надеть'
                    : `${item.fragmentsRequired} фрагментов`}
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
export function ProfilePanel({
  profile,
  error,
  loading,
  equip,
  busy,
  retry,
  achievements = [],
  daily = null,
}: {
  achievements?: Achievement[];
  daily?: DailyChallenge | null;
  profile: Profile | null;
  error: string;
  loading: boolean;
  equip: (id: string) => void;
  busy: string | null;
  retry: () => void;
}) {
  const r = profile?.records;
  return (
    <div className="player-profile">
      {error && <Notice retry={retry}>{error}</Notice>}
      {loading && !profile && <p role="status">Загружаем…</p>}
      {profile && r && (
        <>
          <div className="profile-identity">
            <PlayerAvatar
              name={profile.account.displayName}
              frame={profile.collection.profileFrame}
            />
            <div>
              <h3>{profile.account.displayName}</h3>
              <small>@{profile.account.username}</small>
            </div>
            <strong>{amount(profile.bonusBalance)} Б</strong>
          </div>
          <p className="profile-ranking">
            {profile.tournament.position} место{' '}
            <span>{integer(profile.tournament.points)} очков за сегодня</span>
          </p>
          <DailyCard daily={daily} />
          <section className="personal-records">
            <h3>Рекорды</h3>
            <dl className="record-highlights">
              <div>
                <dt>Лучший выход</dt>
                <dd>{multiplier(r.highestCashoutMultiplier)}</dd>
              </div>
              <div>
                <dt>Крупнейший выигрыш</dt>
                <dd>{amount(r.biggestPayout)} Б</dd>
              </div>
              <div>
                <dt>Лучшая серия</dt>
                <dd>{integer(r.bestWinStreak)}</dd>
              </div>
            </dl>
            <details>
              <summary>Все рекорды</summary>
              <dl className="record-details">
                {[
                  ['Сыграно', integer(r.totalRounds)],
                  ['Победы', integer(r.successfulCashouts)],
                  ['Поражения', integer(r.losses)],
                  ['Побед', `${r.winRate}%`],
                  ['Самый высокий взрыв', multiplier(r.highestCrashMultiplier)],
                  ['Высший уровень', integer(r.highestLevel)],
                  [
                    'Лучший бустер',
                    r.strongestBooster ? `×${r.strongestBooster}` : '—',
                  ],
                  ['Очки за всё время', integer(r.totalPoints)],
                  ['Фрагменты за всё время', integer(r.totalFragments)],
                  ['Серия сейчас', integer(r.currentWinStreak)],
                ].map(([label, value]) => (
                  <div key={label}>
                    <dt>{label}</dt>
                    <dd>{value}</dd>
                  </div>
                ))}
              </dl>
            </details>
          </section>
          <AchievementList items={achievements} />
          <details className="profile-collection">
            <summary>
              Коллекция{' '}
              <span>
                {integer(profile.collection.lifetimeFragments)} фрагментов
              </span>
            </summary>
            <CollectionShelf
              collection={profile.collection}
              equip={equip}
              busy={busy}
            />
          </details>
        </>
      )}
    </div>
  );
}
export function UnlockReveal({
  collection,
  roundId,
}: {
  collection?: Collection;
  roundId: string;
}) {
  const items =
    collection?.items.filter((i) => i.unlockedRoundId === roundId) ?? [];
  if (!items.length) return null;
  return (
    <aside className="unlock-reveal" role="status" aria-label="Новая награда">
      <strong>Открыто!</strong>
      {items.map((item) => (
        <span key={item.id}>
          <CosmeticPreview item={item} />
          <b>{item.name}</b>
        </span>
      ))}
    </aside>
  );
}
