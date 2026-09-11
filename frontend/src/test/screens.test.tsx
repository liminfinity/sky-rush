import { StrictMode } from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BetSelection } from '../features/betting/BetSelection';
import { FlightPanel } from '../features/game/FlightPanel';
import { FlightScene } from '../features/game/FlightScene';
import { ResultScreen } from '../features/game/ResultScreen';
import { BoosterStatus } from '../features/game/BoosterStatus';
import { FragmentProgress } from '../features/rewards/FragmentProgress';
import { Rules } from '../features/rules/Rules';
import {
  balance,
  cashedOut,
  config,
  flying,
  loss,
  round,
  win,
} from './fixtures';

const bets = {
  config,
  balance,
  theme: 'GREEN' as const,
  selected: null,
  chooseTheme: vi.fn(),
  select: vi.fn(),
  start: vi.fn(),
  starting: false,
  pending: false,
  unavailable: false,
};
const panel = { round, cashout: vi.fn(), busy: false, reconnecting: false };
describe('Betting', () => {
  it.each([
    ['RED', 12],
    ['GREEN', 9],
  ] as const)('%s displays %s levels from configuration', (theme, count) => {
    render(<BetSelection {...bets} theme={theme} />);
    expect(
      screen.getByLabelText(`${count} уровней полёта`).children,
    ).toHaveLength(count);
  });
  it('shows four choices and requires a selection', () => {
    render(<BetSelection {...bets} />);
    expect(screen.getAllByRole('button', { name: /^Ставка/ })).toHaveLength(4);
    expect(screen.getByRole('button', { name: /Начать/ })).toBeDisabled();
  });
  it('disables unaffordable bets and their start action', () => {
    render(
      <BetSelection
        {...bets}
        selected="TRIPLE"
        balance={{
          ...balance,
          wallet: { ...balance.wallet, bonusBalance: 15 },
        }}
      />,
    );
    expect(screen.getByRole('button', { name: /Ставка 30/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Ставка 10/ })).toBeEnabled();
    expect(screen.getByRole('button', { name: /Начать/ })).toBeDisabled();
  });
  it('starts only an affordable selected bet and indicates selection', () => {
    const start = vi.fn();
    render(<BetSelection {...bets} selected="DOUBLE" start={start} />);
    expect(screen.getByRole('button', { name: /Ставка 20/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    fireEvent.click(screen.getByRole('button', { name: /Начать/ }));
    expect(start).toHaveBeenCalledOnce();
  });
});
describe('Flight', () => {
  it('cashout is disabled before level 1', () => {
    render(<FlightPanel {...panel} />);
    expect(screen.getByRole('button', { name: /Забрать/ })).toBeDisabled();
    expect(screen.getByText('После первого уровня')).toBeVisible();
  });
  it('cashout follows server permission after level 1', () => {
    render(<FlightPanel {...panel} round={flying} />);
    expect(screen.getByRole('button', { name: /Забрать/ })).toBeEnabled();
  });
  it('busy cashout disables duplicate button clicks', () => {
    const cashout = vi.fn();
    render(<FlightPanel {...panel} round={flying} cashout={cashout} busy />);
    fireEvent.click(screen.getByRole('button', { name: /Забираем/ }));
    expect(cashout).not.toHaveBeenCalled();
  });
  it('keeps fixed payout visible while the live multiplier continues', () => {
    const later = { ...cashedOut, currentMultiplier: 1.65 };
    render(
      <>
        <FlightScene round={later} />
        <FlightPanel {...panel} round={later} />
      </>,
    );
    expect(screen.getByTestId('fixed-payout')).toHaveTextContent('37,50');
    expect(screen.getByTestId('live-multiplier')).toHaveTextContent('1,65');
    expect(screen.getByText(/Могли бы забрать больше/)).toBeVisible();
    expect(screen.getByRole('button', { name: /Забрали/ })).toBeDisabled();
  });
  it('renders server booster WAITING then ACTIVATED', () => {
    const { rerender } = render(<BoosterStatus round={round} />);
    expect(screen.getByText('До ×3 — 2 ур.')).toBeVisible();
    rerender(
      <BoosterStatus
        round={{ ...flying, booster: { ...round.booster, active: true } }}
      />,
    );
    expect(screen.getByTitle('+20 очков')).toBeVisible();
  });
  it('shows missed booster after early cashout', () => {
    render(<BoosterStatus round={cashedOut} />);
    expect(screen.getByText('×3 упущен')).toBeVisible();
  });
  it('shows a burst/loss only after a completed response', () => {
    render(<FlightScene round={loss} />);
    expect(screen.getByText('Шар лопнул')).toBeVisible();
    expect(screen.getByTestId('live-multiplier')).toHaveTextContent('1,75');
    expect(screen.getByText(/Ставка сгорела/)).toBeVisible();
  });
  it('shows only a visual delta from server point totals', () => {
    const { rerender } = render(<FlightPanel {...panel} />);
    rerender(
      <FlightPanel {...panel} round={{ ...flying, earnedPoints: 37 }} />,
    );
    expect(screen.getByLabelText('+37 очков')).toBeVisible();
    expect(screen.getByTestId('round-points')).toHaveTextContent('37');
  });
});
describe('Results and rules', () => {
  it('shows payout, multipliers, points and fragment redemption', () => {
    render(
      <ResultScreen
        round={win}
        balance={balance}
        playAgain={vi.fn()}
        paused={false}
      />,
    );
    expect(screen.getByText('Победа')).toBeVisible();
    expect(screen.getByText(/Обмен 5 фрагментов/)).toBeVisible();
    expect(screen.getByText('37,50')).toBeVisible();
    expect(screen.getByText('Очки')).toBeVisible();
  });
  it('shows lost stake separately from retained rewards', () => {
    render(
      <ResultScreen
        round={loss}
        balance={balance}
        playAgain={vi.fn()}
        paused={false}
      />,
    );
    expect(screen.getByText('Шар лопнул')).toBeVisible();
    expect(screen.getByText('−30,00')).toBeVisible();
    expect(screen.getByText('+1')).toBeVisible();
  });
  it('returns after ten inactive seconds and resets on interaction', async () => {
    vi.useFakeTimers();
    const again = vi.fn();
    render(
      <ResultScreen
        round={win}
        balance={balance}
        playAgain={again}
        paused={false}
      />,
    );
    await act(() => vi.advanceTimersByTimeAsync(9000));
    expect(again).not.toHaveBeenCalled();
    fireEvent.pointerDown(
      screen.getByRole('region', { name: 'Результат раунда' }),
    );
    await act(() => vi.advanceTimersByTimeAsync(9000));
    expect(again).not.toHaveBeenCalled();
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(again).toHaveBeenCalledOnce();
  });
  it('pauses the idle timeout while a dialog is open', async () => {
    vi.useFakeTimers();
    const again = vi.fn();
    render(
      <ResultScreen round={win} balance={balance} playAgain={again} paused />,
    );
    await act(() => vi.advanceTimersByTimeAsync(20000));
    expect(again).not.toHaveBeenCalled();
  });
  it('rules use actual config values', () => {
    render(
      <Rules
        config={{ ...config, points: { ...config.points, pointsPerLevel: 37 } }}
      />,
    );
    expect(screen.getByText(/37 очков за уровень/)).toBeVisible();
    expect(screen.getByText(/Собери 5/)).toBeVisible();
  });
});

it('shows persisted fragment progress and clearly labels current conversion rules', () => {
  render(
    <FragmentProgress
      balance={{ ...balance, skyFragments: 3 }}
      rules={config.reward}
    />,
  );
  expect(screen.getByText('Фрагменты 3 / 5')).toBeVisible();
  expect(screen.getByText(/5 фрагментов/)).toHaveTextContent('5,00 Б');
  expect(screen.getByRole('meter')).toHaveAttribute('value', '3');
});
it('does not invent conversion rules when configuration is unavailable', () => {
  render(<FragmentProgress balance={balance} />);
  expect(screen.queryByRole('meter')).not.toBeInTheDocument();
  expect(screen.getByText(`Фрагменты ${balance.skyFragments}`)).toBeVisible();
  expect(screen.queryByText(/^За \d+ фрагментов:/)).not.toBeInTheDocument();
});

it('shows first-cashout guidance only temporarily, including StrictMode', async () => {
  vi.useFakeTimers();
  sessionStorage.removeItem('skyrush.cashout-hint');
  const view = render(
    <StrictMode>
      <FlightPanel {...panel} round={flying} />
    </StrictMode>,
  );
  expect(screen.getByText(/Нажми «Забрать»/)).toBeVisible();
  await act(() => vi.advanceTimersByTimeAsync(4000));
  expect(screen.queryByText(/Нажми «Забрать»/)).not.toBeInTheDocument();
  view.unmount();
  render(<FlightPanel {...panel} round={flying} />);
  expect(screen.queryByText(/Нажми «Забрать»/)).not.toBeInTheDocument();
});
