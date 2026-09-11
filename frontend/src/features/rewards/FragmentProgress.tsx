import type { Balance, RewardRules } from '../../api/types';
import { amount, integer } from '../../utils/format';
import { FragmentIcon } from '../../components/GameIcon';
export function FragmentProgress({
  balance,
  rules,
}: {
  balance: Balance | null;
  rules?: RewardRules;
}) {
  if (!balance) return null;
  return (
    <aside
      className="fragment-progress"
      aria-label="Фрагменты"
      title="Обмен фрагментов после полёта"
    >
      <FragmentIcon />
      <div>
        <strong>
          Фрагменты {integer(balance.skyFragments)}
          {rules ? ` / ${rules.fragmentsPerBonus}` : ''}
        </strong>
        {rules ? (
          <>
            <meter
              min={0}
              max={rules.fragmentsPerBonus}
              value={Math.min(balance.skyFragments, rules.fragmentsPerBonus)}
              aria-label="Накопленные фрагменты"
            />
            <small>
              За {rules.fragmentsPerBonus} фрагментов:{' '}
              {amount(rules.bonusAmount)} Б
            </small>
          </>
        ) : null}
      </div>
    </aside>
  );
}
