import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { api, ApiError } from '../api/client';
import { HistoryList } from '../features/history/HistoryList';
import { loss, win } from './fixtures';
vi.mock('../api/client', async (original) => ({
  ...(await original<typeof import('../api/client')>()),
  api: { history: vi.fn() },
}));
beforeEach(() => vi.mocked(api.history).mockReset());
it('renders real DTO history fields for wins and losses', async () => {
  vi.mocked(api.history).mockResolvedValue({
    rounds: [win, { ...loss, id: 'loss-id' }],
    limit: 10,
    offset: 0,
  });
  render(<HistoryList revision={0} />);
  expect(await screen.findByText('Победа')).toBeVisible();
  expect(screen.getByText('Проигрыш')).toBeVisible();
  expect(screen.getByText('37,50 Б')).toBeVisible();
  expect(screen.getByText(/Обмен 5 ✦/)).toBeVisible();
});
it('refreshes after completion and supports backend pagination', async () => {
  vi.mocked(api.history).mockResolvedValue({
    rounds: Array.from({ length: 10 }, (_, i) => ({ ...loss, id: String(i) })),
    offset: 0,
    limit: 10,
  });
  const { rerender } = render(<HistoryList revision={0} />);
  await screen.findAllByText('Проигрыш');
  fireEvent.click(screen.getByRole('button', { name: /Раньше/ }));
  await waitFor(() =>
    expect(api.history).toHaveBeenLastCalledWith(10, expect.any(AbortSignal)),
  );
  rerender(<HistoryList revision={1} />);
  await waitFor(() => expect(api.history).toHaveBeenCalledTimes(3));
});
it('shows useful empty and retryable error states', async () => {
  vi.mocked(api.history)
    .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'offline'))
    .mockResolvedValue({ rounds: [], limit: 10, offset: 0 });
  render(<HistoryList revision={0} />);
  expect(await screen.findByRole('alert')).toBeVisible();
  fireEvent.click(screen.getByRole('button', { name: 'Повторить' }));
  expect(await screen.findByText('Полётов пока нет')).toBeVisible();
});
