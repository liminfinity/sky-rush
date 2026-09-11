import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import {
  AchievementList,
  AchievementReveal,
  DailyCard,
  SocialFeed,
} from '../features/social/SocialPanels';
import { useSocial } from '../features/social/useSocial';
import { api } from '../api/client';
import type { Achievement, DailyChallenge, SocialActivity } from '../api/types';
const items: Achievement[] = [
  {
    id: 'FIRST_WIN',
    name: 'Есть!',
    description: 'Забери выигрыш',
    unlockedAt: '2026-09-11T12:00:00Z',
    roundId: 'round',
  },
  {
    id: 'WIN_STREAK_5',
    name: 'Хладнокровие',
    description: '5 побед подряд',
    unlockedAt: null,
    roundId: null,
  },
];
const daily: DailyChallenge = {
  date: '2026-09-11',
  kind: 'WIN_ROUNDS',
  description: 'Победы: 2',
  progress: 1,
  target: 2,
  completed: false,
  rewardFragments: 0,
  rewardRoundId: null,
};
const empty: SocialActivity = {
  online: 1,
  presenceWindowSeconds: 90,
  events: [],
};
describe('real social progression', () => {
  it('shows locked and earned achievements with concise conditions', () => {
    const { container } = render(<AchievementList items={items} />);
    fireEvent.click(screen.getByText('Достижения 1 / 2'));
    expect(screen.getByText('Есть!')).toBeVisible();
    expect(screen.getByText('5 побед подряд')).toBeVisible();
    expect(container.querySelectorAll('.earned')).toHaveLength(1);
    expect(container.querySelectorAll('.locked')).toHaveLength(1);
    expect(container.querySelectorAll('.achievement-art')).toHaveLength(
      items.length,
    );
  });
  it('reveals only achievements unlocked in the completed round', () => {
    const { rerender } = render(
      <AchievementReveal items={items} roundId="other" daily={daily} />,
    );
    expect(screen.queryByText('Достижение открыто')).not.toBeInTheDocument();
    rerender(<AchievementReveal items={items} roundId="round" daily={daily} />);
    expect(screen.getByText('Достижение открыто')).toBeInTheDocument();
    expect(screen.getByText('Есть!')).toBeInTheDocument();
    expect(screen.queryByText('Хладнокровие')).not.toBeInTheDocument();
  });
  it('displays exact server daily progress and completed reward', () => {
    const { rerender } = render(<DailyCard daily={daily} />);
    expect(screen.getByText('1 / 2')).toBeInTheDocument();
    rerender(
      <DailyCard
        daily={{ ...daily, progress: 2, completed: true, rewardFragments: 1 }}
      />,
    );
    expect(screen.getByText('Готово')).toBeInTheDocument();
    expect(screen.getByText('+1 фрагмент')).toBeInTheDocument();
  });
  it('preserves fractional cashout progress instead of rounding into completion', () => {
    render(
      <DailyCard
        daily={{ ...daily, kind: 'CASHOUT_ABOVE', progress: 2.4, target: 2.5 }}
      />,
    );
    expect(screen.getByText('2,4 / 2,5')).toBeInTheDocument();
    expect(screen.queryByText('Готово')).not.toBeInTheDocument();
  });
  it('shows daily reward reveal only for its actual completion round', () => {
    const { rerender } = render(
      <AchievementReveal
        items={[]}
        roundId="round"
        daily={{
          ...daily,
          completed: true,
          rewardFragments: 1,
          rewardRoundId: 'round',
        }}
      />,
    );
    expect(
      screen.getByText('Задание выполнено: +1 фрагмент'),
    ).toBeInTheDocument();
    rerender(
      <AchievementReveal
        items={[]}
        roundId="next"
        daily={{
          ...daily,
          completed: true,
          rewardFragments: 1,
          rewardRoundId: 'round',
        }}
      />,
    );
    expect(
      screen.queryByText('Задание выполнено: +1 фрагмент'),
    ).not.toBeInTheDocument();
  });
  it('never fills a quiet feed with fabricated people and handles zero/one online', () => {
    const { rerender } = render(<SocialFeed data={empty} />);
    expect(screen.getByText('Онлайн: 1')).toBeInTheDocument();
    expect(screen.getByText('Пока тихо')).toBeInTheDocument();
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
    rerender(<SocialFeed data={{ ...empty, online: 0 }} />);
    expect(screen.getByText('Онлайн: 0')).toBeInTheDocument();
  });
  it('renders only received real events and names as text', () => {
    render(
      <SocialFeed
        data={{
          ...empty,
          online: 2,
          events: [
            {
              id: '1',
              displayName: 'Alice',
              kind: 'CASHOUT',
              value: '4.12',
              createdAt: '2026-09-11T12:00:00Z',
            },
            {
              id: '2',
              displayName: 'Bob',
              kind: 'BOOSTER',
              value: '3',
              createdAt: '2026-09-11T11:00:00Z',
            },
          ],
        }}
      />,
    );
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText(/забрал ×4,12/)).toBeInTheDocument();
    expect(screen.getByText(/бустер ×3/)).toBeInTheDocument();
    expect(screen.queryByText('Пока тихо')).not.toBeInTheDocument();
  });
  it('keeps online and events in one collapsible window', () => {
    const { container } = render(<SocialFeed data={empty} />);
    const window = container.querySelector('details')!;
    expect(window).toHaveAttribute('open');
    fireEvent.click(window.querySelector('summary')!);
    expect(window).not.toHaveAttribute('open');
    expect(screen.getByText('Онлайн: 1')).toBeVisible();
    expect(screen.getByText('Пока тихо')).not.toBeVisible();
  });
  it('retains feed when a transient poll fails', () => {
    render(
      <SocialFeed
        data={{
          ...empty,
          events: [
            {
              id: '1',
              displayName: 'Alice',
              kind: 'ACHIEVEMENT',
              value: 'Серия',
              createdAt: '2026-09-11T12:00:00Z',
            },
          ],
        }}
        error="network"
      />,
    );
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Соединяемся…')).toBeInTheDocument();
  });
  it('serializes slow polling, throttles heartbeat and cancels on unmount', async () => {
    vi.useFakeTimers();
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(false);
    let resolve!: (v: SocialActivity) => void;
    const heartbeat = vi
      .spyOn(api, 'heartbeat')
      .mockImplementationOnce(
        () =>
          new Promise((r) => {
            resolve = r;
          }),
      )
      .mockResolvedValue(empty);
    const activity = vi.spyOn(api, 'activity').mockResolvedValue(empty);
    vi.spyOn(api, 'daily').mockResolvedValue(daily);
    vi.spyOn(api, 'achievements').mockResolvedValue(items);
    const { unmount } = renderHook(() => useSocial());
    await act(async () => vi.advanceTimersByTimeAsync(30000));
    expect(heartbeat).toHaveBeenCalledOnce();
    expect(activity).not.toHaveBeenCalled();
    await act(async () => {
      resolve(empty);
      await Promise.resolve();
    });
    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(heartbeat).toHaveBeenCalledTimes(2);
    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(activity).toHaveBeenCalledOnce();
    const signal = activity.mock.calls[0][0];
    unmount();
    expect(signal?.aborted).toBe(true);
    await act(async () => vi.advanceTimersByTimeAsync(60000));
    expect(activity).toHaveBeenCalledOnce();
  });
  it('does not heartbeat from a hidden tab', async () => {
    vi.useFakeTimers();
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(true);
    const heartbeat = vi.spyOn(api, 'heartbeat').mockResolvedValue(empty);
    vi.spyOn(api, 'achievements').mockResolvedValue(items);
    const { unmount } = renderHook(() => useSocial());
    await act(async () => vi.advanceTimersByTimeAsync(30000));
    expect(heartbeat).not.toHaveBeenCalled();
    unmount();
  });
});
