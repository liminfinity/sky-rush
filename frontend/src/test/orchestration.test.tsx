import { StrictMode } from 'react';
import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { api, ApiError } from '../api/client';
import { useGame } from '../features/game/useGame';
import { saveSession } from '../features/game/storage';
import { balance, config, flying, round, win } from './fixtures';
vi.mock('../api/client', async (original) => ({
  ...(await original<typeof import('../api/client')>()),
  api: {
    active: vi.fn(),
    user: vi.fn(),
    balance: vi.fn(),
    config: vi.fn(),
    state: vi.fn(),
    start: vi.fn(),
    cashout: vi.fn(),
  },
}));
beforeEach(() => {
  vi.mocked(api.active).mockReset().mockResolvedValue({ round: null });
  vi.mocked(api.user).mockReset().mockResolvedValue({
    id: 'demo',
    displayName: 'Demo',
    createdAt: round.startedAt,
  });
  vi.mocked(api.balance).mockReset().mockResolvedValue(balance);
  vi.mocked(api.config).mockReset().mockResolvedValue(config);
  vi.mocked(api.state).mockReset().mockResolvedValue(round);
  vi.mocked(api.start).mockReset().mockResolvedValue(round);
});
it('Play Again preserves selected RED theme and clears round storage', async () => {
  vi.mocked(api.start).mockResolvedValue({ ...win, theme: 'RED' });
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.loading).toBe(false));
  act(() => {
    result.current.chooseTheme('RED');
    result.current.setSelected('TRIPLE');
  });
  await act(() => result.current.start());
  expect(result.current.round?.state).toBe('COMPLETED_WIN');
  act(() => result.current.playAgain());
  expect(result.current.theme).toBe('RED');
  expect(result.current.round).toBeNull();
  expect(JSON.parse(localStorage.getItem('skyrush.session.v1')!)).toEqual({});
});
it('restores an active round while current public config is unavailable', async () => {
  saveSession({ roundId: round.id });
  vi.mocked(api.config).mockRejectedValue(
    new ApiError('CONFIG_INVALID', 'bad config', 503),
  );
  vi.mocked(api.state).mockResolvedValue(flying);
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.round?.id).toBe(round.id));
  expect(result.current.round?.canCashout).toBe(true);
  expect(result.current.recovering).toBe(false);
});
it('clears an unknown stored round instead of leaving a stuck screen', async () => {
  saveSession({ roundId: round.id });
  vi.mocked(api.state).mockRejectedValue(
    new ApiError('ROUND_NOT_FOUND', 'gone', 404),
  );
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.recovering).toBe(false));
  expect(result.current.error).toContain('не найден');
  expect(result.current.round).toBeNull();
});
it('retries an uncertain start with its original command ID after reload', async () => {
  vi.mocked(api.start).mockRejectedValueOnce(
    new ApiError('NETWORK_ERROR', 'offline'),
  );
  const first = renderHook(() => useGame());
  await waitFor(() => expect(first.result.current.loading).toBe(false));
  act(() => first.result.current.setSelected('TRIPLE'));
  await act(() => first.result.current.start());
  const original = vi.mocked(api.start).mock.calls[0][0];
  expect(first.result.current.pending?.requestId).toBe(original.requestId);
  first.unmount();
  vi.mocked(api.start).mockResolvedValue(round);
  const second = renderHook(() => useGame());
  await waitFor(() => expect(second.result.current.loading).toBe(false));
  await act(() => second.result.current.start());
  expect(vi.mocked(api.start).mock.calls[1][0]).toEqual(original);
  expect(second.result.current.round?.id).toBe(round.id);
});
it('prevents rapid double start submission', async () => {
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.loading).toBe(false));
  act(() => result.current.setSelected('TRIPLE'));
  await act(async () => {
    await Promise.all([result.current.start(), result.current.start()]);
  });
  expect(api.start).toHaveBeenCalledOnce();
});
it('surfaces initial API errors and allows a successful retry', async () => {
  vi.mocked(api.config).mockRejectedValueOnce(
    new ApiError('NETWORK_ERROR', 'offline'),
  );
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.loading).toBe(false));
  expect(result.current.dataError).toBeTruthy();
  await act(() => result.current.refreshData());
  expect(result.current.config).toEqual(config);
  expect(result.current.dataError).toBe('');
});
it('accepts a later fractional-second server timestamp after a whole second', async () => {
  saveSession({ roundId: round.id });
  vi.mocked(api.state)
    .mockResolvedValueOnce(flying)
    .mockResolvedValue({
      ...flying,
      currentMultiplier: 1.35,
      serverTime: '2026-09-11T12:00:01.400Z',
    });
  const { result } = renderHook(() => useGame());
  await waitFor(() =>
    expect(result.current.round?.currentMultiplier).toBe(1.25),
  );
  await waitFor(() =>
    expect(result.current.round?.currentMultiplier).toBe(1.35),
  );
});

it('discovers the server active round without local recovery data', async () => {
  vi.mocked(api.active).mockResolvedValue({ round: flying });
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.round?.id).toBe(flying.id));
});
it('instant repeat sends same selections with a fresh request ID and current config', async () => {
  saveSession({ roundId: win.id });
  vi.mocked(api.state).mockResolvedValue(win);
  const { result } = renderHook(() => useGame());
  await waitFor(() =>
    expect(result.current.round?.state).toBe('COMPLETED_WIN'),
  );
  vi.mocked(api.start).mockResolvedValue({
    ...round,
    id: '00000000-0000-0000-0000-000000000088',
  });
  await act(() => result.current.start(win));
  expect(api.config).toHaveBeenCalledTimes(2);
  expect(api.start).toHaveBeenCalledWith(
    expect.objectContaining({
      theme: win.theme,
      betOptionId: win.betOptionId,
      requestId: expect.any(String) as unknown,
    }),
    expect.any(AbortSignal),
  );
  expect(result.current.round?.state).toBe('ACTIVE');
});
it('instant repeat rejects a newly unaffordable bet without submitting', async () => {
  const { result } = renderHook(() => useGame());
  await waitFor(() => expect(result.current.loading).toBe(false));
  vi.mocked(api.balance).mockResolvedValue({
    ...balance,
    wallet: { ...balance.wallet, bonusBalance: 1 },
  });
  await act(() => result.current.start(win));
  expect(api.start).not.toHaveBeenCalled();
  expect(result.current.error).toContain('Не хватает');
});

it('ignores aborted discovery from a previous Strict Mode lifecycle', async () => {
  vi.mocked(api.active)
    .mockImplementationOnce(
      (signal) =>
        new Promise((_, reject) =>
          signal?.addEventListener('abort', () =>
            reject(new DOMException('Aborted', 'AbortError')),
          ),
        ),
    )
    .mockResolvedValue({ round: null });
  const { result } = renderHook(() => useGame(), { wrapper: StrictMode });
  await waitFor(() => expect(result.current.loading).toBe(false));
  expect(result.current.error).toBe('');
  expect(api.active).toHaveBeenCalledTimes(2);
});
