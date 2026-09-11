import { isCompleted, type Round } from '../../api/types';
export function BoosterStatus({ round }: { round: Round }) {
  const b = round.booster;
  const state =
    b.multiplier === 1
      ? 'none'
      : b.active
        ? 'active'
        : round.cashoutAt || isCompleted(round)
          ? 'missed'
          : 'waiting';
  return (
    <div
      className={`booster-status ${state}`}
      data-state={state}
      role="status"
      title={
        state === 'active'
          ? `+${round.pointsRules.boosterActivationBonuses[String(b.multiplier)]} очков`
          : state === 'missed'
            ? 'Не влияет на выплату'
            : state === 'none'
              ? 'Без бустера'
              : `Бустер на уровне ${b.level}`
      }
    >
      {state === 'none'
        ? 'Без бустера'
        : state === 'active'
          ? `Бустер ×${b.multiplier}!`
          : state === 'missed'
            ? `×${b.multiplier} упущен`
            : `До ×${b.multiplier} — ${Math.max(0, (b.level ?? 0) - round.completedLevel)} ур.`}
    </div>
  );
}
