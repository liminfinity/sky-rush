import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import type { Profile } from '../api/types';
import {
  ProfilePanel,
  CollectionShelf,
  UnlockReveal,
} from '../features/profile/ProfilePanel';
import { CosmeticsContext } from '../features/profile/Cosmetics';
import { useProfile } from '../features/profile/useProfile';
import { AppShell } from '../components/AppShell';
import { Balloon } from '../components/Balloon';
import { ResultScreen } from '../features/game/ResultScreen';
import { LiveRanking } from '../features/competition/LiveRanking';
import { api, ApiError } from '../api/client';
import { balance, win, loss } from './fixtures';
const profile: Profile = {
  account: {
    id: 'alice',
    username: 'alice',
    displayName: 'Алиса',
    createdAt: '2026-09-11',
    evaluator: false,
  },
  bonusBalance: 1125,
  fragmentBalance: 1,
  tournament: {
    id: 'alice',
    name: 'Алиса',
    position: 2,
    points: 120,
    currentPlayer: true,
  },
  records: {
    totalRounds: 3,
    successfulCashouts: 3,
    losses: 0,
    winRate: 100,
    highestCashoutMultiplier: 4.82,
    highestCrashMultiplier: 8.1,
    biggestPayout: 482,
    highestLevel: 8,
    strongestBooster: 3,
    totalPoints: 120,
    totalFragments: 6,
    currentWinStreak: 3,
    bestWinStreak: 3,
  },
  collection: {
    lifetimeFragments: 6,
    nextThreshold: 10,
    fragmentsToNext: 4,
    nextItemName: 'Бронза',
    balloonSkin: 'classic',
    profileFrame: 'plain',
    items: [
      {
        id: 'classic',
        kind: 'BALLOON',
        name: 'Классика',
        fragmentsRequired: 0,
        unlocked: true,
        equipped: true,
        unlockedAt: '2026-09-11',
        unlockedRoundId: null,
      },
      {
        id: 'constellations',
        kind: 'BALLOON',
        name: 'Созвездия',
        fragmentsRequired: 5,
        unlocked: true,
        equipped: false,
        unlockedAt: '2026-09-11',
        unlockedRoundId: win.id,
      },
      {
        id: 'bronze',
        kind: 'FRAME',
        name: 'Бронза',
        fragmentsRequired: 10,
        unlocked: false,
        equipped: false,
        unlockedAt: null,
        unlockedRoundId: null,
      },
    ],
  },
};
describe('player profile and collection', () => {
  it('opens profile from compact identity', () => {
    const open = vi.fn();
    render(
      <AppShell
        theme="GREEN"
        user={profile.account}
        balance={balance}
        onRules={() => {}}
        onHistory={() => {}}
        onProfile={open}
      >
        Game
      </AppShell>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Профиль' }));
    expect(open).toHaveBeenCalledOnce();
  });
  it('renders server records and expands remaining lifetime stats', () => {
    render(
      <ProfilePanel
        profile={profile}
        error=""
        loading={false}
        equip={() => {}}
        busy={null}
        retry={() => {}}
      />,
    );
    expect(screen.getByText('×4,82')).toBeInTheDocument();
    expect(screen.getByText('482,00 Б')).toBeInTheDocument();
    expect(screen.getByText('2 место')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Все рекорды'));
    expect(screen.getByText('Очки за всё время')).toBeVisible();
  });
  it('shows locked, unlocked and equipped items and server progress', () => {
    const equip = vi.fn();
    render(
      <CollectionShelf
        collection={profile.collection}
        equip={equip}
        busy={null}
      />,
    );
    expect(screen.getByText('6 / 10 фрагментов')).toBeInTheDocument();
    expect(screen.getByText('Ещё 4 до «Бронза»')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Закрыто: Бронза' }),
    ).toBeDisabled();
    expect(
      screen.getByRole('button', { name: 'Выбрано: Классика' }),
    ).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Надеть: Созвездия' }));
    expect(equip).toHaveBeenCalledWith('constellations');
  });
  it('disables equips during request and presents errors without hiding profile', () => {
    render(
      <ProfilePanel
        profile={profile}
        error="Этот предмет ещё закрыт"
        loading={false}
        equip={() => {}}
        busy="constellations"
        retry={() => {}}
      />,
    );
    fireEvent.click(screen.getByText(/^Коллекция/, { selector: 'summary' }));
    expect(
      screen.getByRole('button', { name: 'Надеть: Созвездия' }),
    ).toBeDisabled();
    expect(screen.getByText('Этот предмет ещё закрыт')).toBeInTheDocument();
  });
  it('reveals only items earned by this completed round', () => {
    const { rerender } = render(
      <UnlockReveal collection={profile.collection} roundId="other" />,
    );
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    rerender(<UnlockReveal collection={profile.collection} roundId={win.id} />);
    expect(screen.getByText('Открыто!')).toBeInTheDocument();
    expect(screen.getByText('Созвездия')).toBeInTheDocument();
    expect(screen.queryByText('Классика')).not.toBeInTheDocument();
  });
  it('applies skin and current-player frame without changing theme data', () => {
    const { container } = render(
      <CosmeticsContext.Provider
        value={{ balloonSkin: 'constellations', profileFrame: 'bronze' }}
      >
        <Balloon />
        <LiveRanking
          ranking={{
            entries: [
              profile.tournament,
              {
                id: 'bob',
                name: 'Боб',
                points: 1,
                currentPlayer: false,
                position: 3,
              },
            ],
          }}
        />
      </CosmeticsContext.Provider>,
    );
    expect(
      container.querySelector('svg[data-skin="constellations"]'),
    ).toBeInTheDocument();
    expect(container.querySelectorAll('[data-frame="bronze"]')).toHaveLength(1);
  });
  it('compares exact server cashout and crash, loss has no cashout comparison', () => {
    const { container, rerender } = render(
      <ResultScreen
        round={{ ...win, cashoutMultiplier: 2.45, crashMultiplier: 5.31 }}
        balance={balance}
        playAgain={() => {}}
        paused
      />,
    );
    expect(screen.getByText('×2,45')).toBeInTheDocument();
    expect(screen.getByText('×5,31')).toBeInTheDocument();
    expect(container.querySelector('.comparison-path')).toBeInTheDocument();
    rerender(
      <ResultScreen
        round={loss}
        balance={balance}
        playAgain={() => {}}
        paused
      />,
    );
    expect(container.querySelector('.comparison-path')).not.toBeInTheDocument();
  });
  it('equips via API once and refreshes authoritative selection', async () => {
    const updated = {
      ...profile,
      collection: { ...profile.collection, balloonSkin: 'constellations' },
    };
    vi.spyOn(api, 'profile')
      .mockResolvedValueOnce(profile)
      .mockResolvedValue(updated);
    let resolve!: (value: typeof profile.collection) => void;
    const equip = vi.spyOn(api, 'equip').mockImplementation(
      () =>
        new Promise((r) => {
          resolve = r;
        }),
    );
    const { result } = renderHook(() => useProfile());
    await waitFor(() => expect(result.current.profile).toEqual(profile));
    act(() => {
      void result.current.equip('constellations');
      void result.current.equip('constellations');
    });
    expect(equip).toHaveBeenCalledTimes(1);
    await act(async () => {
      resolve(updated.collection);
      await Promise.resolve();
    });
    await waitFor(() =>
      expect(result.current.profile?.collection.balloonSkin).toBe(
        'constellations',
      ),
    );
  });
  it('refreshes records after completion and handles network failure', async () => {
    const get = vi
      .spyOn(api, 'profile')
      .mockResolvedValueOnce(profile)
      .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'internal'));
    const { result, rerender } = renderHook(({ id }) => useProfile(id), {
      initialProps: { id: undefined as string | undefined },
    });
    await waitFor(() => expect(result.current.profile).toEqual(profile));
    rerender({ id: win.id });
    await waitFor(() =>
      expect(result.current.error).toBe(
        'Не удалось подключиться. Попробуй ещё раз.',
      ),
    );
    expect(result.current.profile).toEqual(profile);
    expect(get).toHaveBeenCalledTimes(2);
  });
  it('keeps an equip rejection visible without changing selected cosmetics', async () => {
    vi.spyOn(api, 'profile').mockResolvedValue(profile);
    vi.spyOn(api, 'equip').mockRejectedValue(
      new ApiError('COSMETIC_LOCKED', 'internal', 409),
    );
    const { result } = renderHook(() => useProfile());
    await waitFor(() => expect(result.current.profile).toEqual(profile));
    await act(async () => {
      await result.current.equip('bronze');
    });
    expect(result.current.error).toBe('Этот предмет ещё закрыт');
    expect(result.current.profile?.collection.profileFrame).toBe('plain');
    expect(result.current.equipping).toBeNull();
  });
  it('discards an older profile response and aborts work on unmount', async () => {
    let resolve!: (p: Profile) => void;
    const get = vi
      .spyOn(api, 'profile')
      .mockImplementationOnce(
        () =>
          new Promise((r) => {
            resolve = r;
          }),
      )
      .mockResolvedValue(profile);
    const { result, unmount } = renderHook(() => useProfile());
    await act(async () => {
      await result.current.refresh();
    });
    await act(async () => {
      resolve({ ...profile, bonusBalance: 1 });
      await Promise.resolve();
    });
    expect(result.current.profile?.bonusBalance).toBe(1125);
    const signal = get.mock.calls.at(-1)?.[0];
    unmount();
    expect(signal?.aborted).toBe(true);
  });
});
