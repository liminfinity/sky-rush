import { act, renderHook } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { api, ApiError } from '../api/client';
import { useRoundPolling } from '../features/game/useRoundPolling';
import { flying, cashedOut, loss } from './fixtures';
vi.mock('../api/client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/client')>()),
  api: { state: vi.fn(), cashout: vi.fn() },
}));
beforeEach(() => {
  vi.useFakeTimers();
  vi.mocked(api.state).mockReset();
  vi.mocked(api.cashout).mockReset();
});
function deferred<T>() {
  let resolve!: (v: T) => void;
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}
it('does not overlap polls and aborts on unmount', async () => {
  const wait = deferred<typeof flying>();
  vi.mocked(api.state).mockReturnValue(wait.promise);
  const { unmount } = renderHook(() =>
    useRoundPolling(flying, vi.fn(), vi.fn()),
  );
  await act(() => vi.advanceTimersByTimeAsync(450));
  await act(() => vi.advanceTimersByTimeAsync(5000));
  expect(api.state).toHaveBeenCalledOnce();
  const signal = vi.mocked(api.state).mock.calls[0][1];
  unmount();
  expect(signal?.aborted).toBe(true);
});
it('serializes cashout behind a poll and prevents rapid duplicate commands', async () => {
  const wait = deferred<typeof flying>();
  vi.mocked(api.state)
    .mockReturnValueOnce(wait.promise)
    .mockResolvedValue(cashedOut);
  vi.mocked(api.cashout).mockResolvedValue(cashedOut);
  const accept = vi.fn();
  const { result } = renderHook(() => useRoundPolling(flying, accept, vi.fn()));
  await act(() => vi.advanceTimersByTimeAsync(450));
  act(() => {
    result.current.cashout();
    result.current.cashout();
  });
  expect(api.cashout).not.toHaveBeenCalled();
  await act(async () => {
    wait.resolve(flying);
    await wait.promise;
  });
  await act(() => vi.advanceTimersByTimeAsync(1));
  expect(api.cashout).toHaveBeenCalledOnce();
  expect(accept).toHaveBeenLastCalledWith(cashedOut);
});
it('retains flight and recovers from a transient polling failure', async () => {
  vi.mocked(api.state)
    .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'offline'))
    .mockResolvedValue(flying);
  const accept = vi.fn();
  const { result } = renderHook(() => useRoundPolling(flying, accept, vi.fn()));
  await act(() => vi.advanceTimersByTimeAsync(450));
  expect(result.current.connectionError).toBeTruthy();
  expect(accept).not.toHaveBeenCalled();
  await act(() => vi.advanceTimersByTimeAsync(450));
  expect(result.current.connectionError).toBe('');
  expect(accept).toHaveBeenCalledWith(flying);
});
it('stops polling after completed response', async () => {
  vi.mocked(api.state).mockResolvedValue(loss);
  renderHook(() => useRoundPolling(flying, vi.fn(), vi.fn()));
  await act(() => vi.advanceTimersByTimeAsync(5000));
  expect(api.state).toHaveBeenCalledOnce();
});
it('reconciles uncertain cashout with state instead of automatically retrying POST', async () => {
  vi.mocked(api.cashout).mockRejectedValue(
    new ApiError('NETWORK_ERROR', 'offline'),
  );
  vi.mocked(api.state).mockResolvedValue(cashedOut);
  const { result } = renderHook(() =>
    useRoundPolling(flying, vi.fn(), vi.fn()),
  );
  act(() => result.current.cashout());
  await act(() => vi.advanceTimersByTimeAsync(1));
  expect(api.cashout).toHaveBeenCalledOnce();
  expect(api.state).toHaveBeenCalledOnce();
});
