import { useEffect, useRef, useState, type ReactNode } from 'react';
import { FragmentIcon, ScoreIcon } from '../../components/GameIcon';
import { FragmentProgress } from '../rewards/FragmentProgress';
import type { Balance, Round, RewardRules } from '../../api/types';
import { amount, integer, multiplier } from '../../utils/format';

export function ResultScreen({
  unlock,
  round,
  balance,
  playAgain,
  paused,
  rewardRules,
  repeat,
  repeating,
  onIdle,
}: {
  unlock?: ReactNode;
  round: Round;
  balance: Balance | null;
  playAgain: () => void;
  paused: boolean;
  rewardRules?: RewardRules;
  repeat?: () => void;
  repeating?: boolean;
  onIdle?: () => void;
}) {
  const win = round.state === 'COMPLETED_WIN';
  const [seconds, setSeconds] = useState(10);
  const [activity, setActivity] = useState(0);
  const action = useRef(onIdle ?? playAgain);
  action.current = onIdle ?? playAgain;
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  useEffect(() => {
    setSeconds(10);
    if (paused) return;
    const timer = setInterval(
      () => setSeconds((value) => Math.max(0, value - 1)),
      1000,
    );
    return () => clearInterval(timer);
  }, [paused, activity]);
  useEffect(() => {
    if (seconds === 0 && !paused) action.current();
  }, [seconds, paused]);
  return (
    <section
      className={`result-screen ${win ? 'win' : 'loss'}`}
      onPointerDownCapture={() => setActivity((v) => v + 1)}
      onKeyDownCapture={() => setActivity((v) => v + 1)}
      aria-label="Результат раунда"
    >
      <div className="reward-aura" aria-hidden="true" />
      <h1 ref={heading} tabIndex={-1}>
        {win ? 'Победа' : 'Шар лопнул'}
      </h1>
      <div className="result-amount">
        {!win && <span>Ставка сгорела</span>}
        <strong>
          {win ? amount(round.payout) : `−${amount(round.stake)}`}{' '}
          <small>Б</small>
        </strong>
      </div>
      <div className={`result-multipliers ${win ? 'win-comparison' : ''}`}>
        {win && (
          <span>
            Забрали <b>{multiplier(round.cashoutMultiplier)}</b>
          </span>
        )}
        {win && <i className="comparison-path" aria-hidden="true" />}
        <span>
          Лопнул на <b>{multiplier(round.crashMultiplier)}</b>
        </span>
      </div>
      <div className="result-loot">
        <div className="score-loot">
          <ScoreIcon />
          <strong>+{integer(round.earnedPoints)}</strong>
          <span>Очки</span>
        </div>
        <div className="fragment-loot">
          <FragmentIcon />
          <strong>+{round.reward?.fragmentsGranted ?? 0}</strong>
          <span>Фрагменты</span>
        </div>
      </div>
      {round.reward && round.reward.fragmentsRedeemed > 0 && (
        <p className="reward-conversion">
          Обмен {round.reward.fragmentsRedeemed} фрагментов за +
          {amount(round.reward.bonusBalanceGranted)} Б
        </p>
      )}
      <FragmentProgress balance={balance} rules={rewardRules} />
      {unlock}
      <div className="result-actions">
        <button
          type="button"
          className="primary-button play-again"
          onClick={playAgain}
        >
          Играть снова
        </button>
        {repeat && (
          <button
            type="button"
            className="quiet-button repeat-button"
            disabled={repeating}
            onClick={repeat}
          >
            {repeating ? 'Запускаем…' : 'Повторить ставку'}
          </button>
        )}
      </div>
      <p className="small-note idle-note">
        {paused
          ? ''
          : `К выбору ${onIdle ? 'шара' : 'ставки'} через ${seconds} с`}
      </p>
    </section>
  );
}
