import { act, renderHook } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { useGameAudio } from '../features/game/useGameAudio';

afterEach(() => vi.unstubAllGlobals());

it('handles an audio device disappearing during unmount without an unhandled rejection', async () => {
  const close = vi
    .fn<() => Promise<void>>()
    .mockRejectedValue(new Error('Device gone'));
  vi.stubGlobal(
    'AudioContext',
    class {
      state = 'running';
      resume = () => Promise.resolve();
      close = close;
    },
  );
  const { result, unmount } = renderHook(() => useGameAudio(null));
  await act(() => result.current.toggle());
  expect(result.current.enabled).toBe(true);
  unmount();
  await Promise.resolve();
  expect(close).toHaveBeenCalledOnce();
});
