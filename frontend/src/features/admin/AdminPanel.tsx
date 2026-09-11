import { useEffect, useState } from 'react';
import { api, adminErrorMessage } from '../../api/client';
import type {
  ConfigSnapshot,
  GameConfiguration,
  PrototypeConfiguration,
} from '../../api/types';
import { Notice } from '../../components/Notice';
const help: Record<string, string> = {
  gameId: 'Идентификатор игры',
  name: 'Название',
  type: 'Тип (фиксирован)',
  active: 'Разрешить новые раунды',
  themes:
    'Пороги относятся к базовому множителю. RED: 12, GREEN: 9 — число уровней фиксировано.',
  levelThresholds: 'Строго возрастающие пороги, не выше максимума взрыва',
  boosterPositionWeights:
    'Вероятность = вес / сумма весов; ноль исключает уровень',
  betOptions: 'Ровно четыре ставки с усилением ×1–×4',
  stake: 'Ставка в бонусах, 2 знака после запятой',
  boosterMultiplier: 'Множитель бустера: 1, 2, 3, 4 — каждый один раз',
  crashModel: 'Равномерная модель: минимум включён, максимум исключён',
  minMultiplier: 'Минимум взрыва: >1, максимум 100',
  maxMultiplier: 'Максимум взрыва: больше минимума, до 100',
  growth: 'Рост базового множителя по серверному времени',
  multiplierPerSecond: 'Рост в секунду: 0.01–100; для демо обычно 0.25–1',
  points: 'Игровые очки, не бонусный баланс',
  pointsPerLevel: 'Очки за уровень: 0–1000000',
  pointsCashoutBonus: 'Бонус очков за cashout',
  boosterActivationBonuses: 'Очки при активации; ×1 обязательно 0',
  reward: 'Sky Fragments: грант и автоматический обмен',
  fragmentsWin: 'Фрагменты за победу',
  fragmentsLoss: 'Фрагменты за проигрыш',
  fragmentsPerBonus: 'Порог обмена: 1–1000000',
  bonusAmount: 'Бонусный баланс за обмен',
  upsell:
    'Имитация билетов. Предложения сохраняют свои цены до истечения срока.',
  enabled: 'Включить предложение',
  minWinAmount: 'Минимальная выплата для предложения, 0.01–1000000',
  popupTimeoutSeconds: 'Время предложения: 3–60 секунд',
  ticketPrice: 'Цена билета: 0.01–1000000 бонусов',
  maxTickets: 'Максимум билетов: 1–100',
  payoutFraction: 'Доля выплаты для бюджета: больше 0, до 1 (например 0.10)',
};
function Fields({
  value,
  template,
  path = [],
  change,
}: {
  value: unknown;
  template: unknown;
  path?: string[];
  change: (path: string[], value: unknown) => void;
}) {
  if (value !== null && typeof value === 'object')
    return (
      <>
        {Object.entries(value as Record<string, unknown>).map(([key, item]) => {
          const next = [...path, key],
            label = help[key] || key;
          const original = (template as Record<string, unknown>)[key];
          const numeric = typeof original === 'number';
          return typeof item === 'object' && item !== null ? (
            <fieldset key={key}>
              <legend>{key}</legend>
              {help[key] && <p>{help[key]}</p>}
              <Fields
                value={item}
                template={original}
                path={next}
                change={change}
              />
            </fieldset>
          ) : (
            <label className="admin-field" key={key}>
              <span>
                {label}
                {/^\d+$/.test(key)
                  ? ` (${path.at(-1) === 'boosterActivationBonuses' ? 'бустер ×' + key : 'позиция ' + (Number(key) + 1)})`
                  : ''}
              </span>
              {typeof item === 'boolean' ? (
                <input
                  type="checkbox"
                  checked={item}
                  onChange={(e) => change(next, e.target.checked)}
                />
              ) : (
                <input
                  aria-label={next.join('.')}
                  type={numeric ? 'number' : 'text'}
                  step="any"
                  min={numeric ? 0 : undefined}
                  required
                  readOnly={key === 'type'}
                  value={
                    typeof item === 'string' || typeof item === 'number'
                      ? item
                      : ''
                  }
                  onChange={(e) =>
                    change(
                      next,
                      numeric
                        ? e.target.value === ''
                          ? ''
                          : Number(e.target.value)
                        : e.target.value,
                    )
                  }
                />
              )}
            </label>
          );
        })}
      </>
    );
  return null;
}
function ConfigForm<T extends GameConfiguration | PrototypeConfiguration>({
  initial,
  title,
  save,
  onSaved,
}: {
  initial: ConfigSnapshot<T>;
  title: string;
  save: (body: ConfigSnapshot<T>) => Promise<ConfigSnapshot<T>>;
  onSaved: () => void;
}) {
  const [draft, setDraft] = useState(initial),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(''),
    [success, setSuccess] = useState('');
  const change = (path: string[], value: unknown) => {
    setSuccess('');
    setDraft((old) => {
      const next = structuredClone(old);
      let node = next.configuration as unknown as Record<string, unknown>;
      for (const key of path.slice(0, -1))
        node = node[key] as Record<string, unknown>;
      node[path[path.length - 1]] = value;
      return next;
    });
  };
  return (
    <form
      className="admin-form"
      onSubmit={(e) => {
        e.preventDefault();
        void (async () => {
          if (busy) return;
          setBusy(true);
          setError('');
          setSuccess('');
          try {
            setDraft(await save(draft));
            setSuccess('Сохранено. Новые раунды используют новые правила.');
            onSaved();
          } catch (err) {
            setError(adminErrorMessage(err));
          } finally {
            setBusy(false);
          }
        })();
      }}
    >
      <h3>{title}</h3>
      <Fields
        value={draft.configuration}
        template={initial.configuration}
        change={change}
      />
      {error && <Notice>{error}</Notice>}
      {success && <p role="status">{success}</p>}
      <button type="submit" className="primary-button" disabled={busy}>
        {busy ? 'Сохраняем…' : `Сохранить: ${title}`}
      </button>
    </form>
  );
}
export function AdminPanel({ onSaved }: { onSaved: () => void }) {
  const [game, setGame] = useState<ConfigSnapshot<GameConfiguration> | null>(
      null,
    ),
    [prototype, setPrototype] =
      useState<ConfigSnapshot<PrototypeConfiguration> | null>(null),
    [error, setError] = useState(''),
    [revision, setRevision] = useState(0);
  useEffect(() => {
    const c = new AbortController();
    void Promise.all([api.adminGame(c.signal), api.adminPrototype(c.signal)])
      .then(([g, p]) => {
        setGame(g);
        setPrototype(p);
        setError('');
      })
      .catch((e) => {
        if (!c.signal.aborted) setError(adminErrorMessage(e));
      });
    return () => c.abort();
  }, [revision]);
  return (
    <section>
      <p>
        <b>Для жюри аккаунт demo.</b> Сохраняйте разделы отдельно. Активные
        раунды сохраняют исходные правила.
      </p>
      <button
        type="button"
        className="quiet-button"
        onClick={() => {
          setGame(null);
          setPrototype(null);
          setRevision((v) => v + 1);
        }}
      >
        Перезагрузить формы (сбросить правки)
      </button>
      {error && <Notice>{error}</Notice>}
      {!game || !prototype ? (
        <p role="status">Загружаем настройки…</p>
      ) : (
        <>
          <ConfigForm
            key={`game-${revision}`}
            initial={game}
            title="Математика и награды"
            save={api.saveGame}
            onSaved={onSaved}
          />
          <ConfigForm
            key={`prototype-${revision}`}
            initial={prototype}
            title="Игра и билеты"
            save={api.savePrototype}
            onSaved={onSaved}
          />
        </>
      )}
    </section>
  );
}
