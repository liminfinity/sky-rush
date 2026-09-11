import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';
import { api, ApiError } from '../api/client';
import { ThemeSelection } from '../features/betting/ThemeSelection';
import { LiveRanking } from '../features/competition/LiveRanking';
import { TournamentTable } from '../features/competition/TournamentTable';
import { AdminPanel } from '../features/admin/AdminPanel';
import { UpsellPrompt } from '../features/upsell/UpsellPrompt';
import { ResultScreen } from '../features/game/ResultScreen';
import { readSession, saveSession } from '../features/game/storage';
import { config, win, loss, balance } from './fixtures';
vi.mock('../api/client', async (original) => ({
  ...(await original<typeof import('../api/client')>()),
  api: {
    tournament: vi.fn(),
    adminGame: vi.fn(),
    adminPrototype: vi.fn(),
    saveGame: vi.fn(),
    savePrototype: vi.fn(),
    offer: vi.fn(),
    decide: vi.fn(),
  },
}));
const entries = [
  {
    id: 'alice',
    name: 'Al***',
    points: 100,
    position: 1,
    currentPlayer: false,
  },
  { id: 'you', name: 'Вы', points: 10, position: 2, currentPlayer: true },
];
const gameConfig = {
  ...config,
  themes: {
    RED: {
      levelThresholds: config.themes.RED.levelThresholds,
      boosterPositionWeights: Array(12).fill(1),
    },
    GREEN: {
      levelThresholds: config.themes.GREEN.levelThresholds,
      boosterPositionWeights: Array(9).fill(1),
    },
  },
  crashModel: { minMultiplier: 1.1, maxMultiplier: 12 },
  growth: { multiplierPerSecond: 0.25 },
};
const prototype = {
  gameId: 'sky-rush',
  name: 'SkyRush',
  type: 'BALLOON_CRASH',
  active: true,
  upsell: {
    enabled: true,
    minWinAmount: 50,
    popupTimeoutSeconds: 10,
    ticketPrice: 5,
    maxTickets: 5,
    payoutFraction: 0.1,
  },
};
const offer = {
  id: 'offer',
  roundId: win.id,
  quantity: 2,
  unitPrice: 5,
  total: 10,
  expiresAt: '2026-09-11T12:01:10Z',
  remainingSeconds: 10,
  status: 'OFFERED' as const,
};
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.offer).mockResolvedValue({ offer, tickets: 0 });
  vi.mocked(api.decide).mockResolvedValue({
    offer: { ...offer, status: 'PURCHASED' },
    tickets: 2,
  });
  vi.mocked(api.tournament).mockResolvedValue({
    name: 'Demo',
    status: 'ACTIVE',
    remainingSeconds: 120,
    endsAt: win.completedAt!,
    entries,
    currentPlayer: entries[1],
  });
  vi.mocked(api.adminGame).mockResolvedValue({
    version: 'v1',
    configuration: gameConfig,
  });
  vi.mocked(api.adminPrototype).mockResolvedValue({
    version: 'p1',
    configuration: prototype,
  });
  vi.mocked(api.saveGame).mockResolvedValue({
    version: 'v2',
    configuration: gameConfig,
  });
});
it('theme screen has two balloons with correct counts and opens selected theme', () => {
  const choose = vi.fn();
  render(<ThemeSelection config={config} choose={choose} />);
  expect(screen.getByText('12 уровней')).toBeVisible();
  expect(screen.getByText('9 уровней')).toBeVisible();
  fireEvent.click(screen.getByRole('button', { name: 'Выбрать RED' }));
  expect(choose).toHaveBeenCalledWith('RED');
});
it('live ranking renders only server order and updates current player', () => {
  const view = render(<LiveRanking ranking={{ entries }} />);
  expect(screen.getAllByRole('listitem')[1]).toHaveAttribute(
    'aria-current',
    'true',
  );
  view.rerender(
    <LiveRanking
      ranking={{
        entries: [
          { ...entries[1], points: 110, position: 1 },
          { ...entries[0], position: 2 },
        ],
      }}
    />,
  );
  expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('110');
});
it('tournament shows current player and supports server name masking', async () => {
  render(<TournamentTable />);
  await screen.findByText(/\(Ты\)/);
  expect(screen.getAllByText(/\(Ты\)/)).toHaveLength(1);
  fireEvent.click(screen.getByRole('checkbox'));
  await waitFor(() =>
    expect(api.tournament).toHaveBeenLastCalledWith(
      false,
      expect.any(AbortSignal),
    ),
  );
});
it('admin provides typed fields and surfaces server validation', async () => {
  vi.mocked(api.saveGame).mockRejectedValue(
    new ApiError('CONFIG_VALIDATION', 'points invalid', 400),
  );
  render(<AdminPanel onSaved={vi.fn()} />);
  const input = await screen.findByLabelText('points.pointsPerLevel');
  expect(input).toHaveAttribute('type', 'number');
  fireEvent.change(input, { target: { value: '37' } });
  fireEvent.click(
    screen.getByRole('button', { name: 'Сохранить: Математика и награды' }),
  );
  await screen.findByText('points invalid');
  expect(api.saveGame).toHaveBeenCalledWith(
    expect.objectContaining({
      configuration: expect.objectContaining({
        points: expect.objectContaining({ pointsPerLevel: 37 }) as unknown,
      }) as unknown,
    }),
  );
});
it('admin confirms successful apply', async () => {
  const saved = vi.fn();
  render(<AdminPanel onSaved={saved} />);
  await screen.findByLabelText('points.pointsPerLevel');
  fireEvent.click(
    screen.getByRole('button', { name: 'Сохранить: Математика и награды' }),
  );
  await screen.findByText(/Сохранено/);
  expect(saved).toHaveBeenCalledOnce();
});
it('upsell accept sends an intention once and refreshes balance', async () => {
  const balanceChanged = vi.fn();
  render(
    <UpsellPrompt round={win} onOpen={vi.fn()} onBalance={balanceChanged} />,
  );
  const button = await screen.findByRole('button', { name: 'Купить' });
  fireEvent.click(button);
  fireEvent.click(button);
  await waitFor(() => expect(balanceChanged).toHaveBeenCalledOnce());
  expect(api.decide).toHaveBeenCalledOnce();
  expect(api.decide).toHaveBeenCalledWith('offer', expect.any(String), true);
});
it('upsell decline closes and is not shown again this session', async () => {
  const view = render(
    <UpsellPrompt round={win} onOpen={vi.fn()} onBalance={vi.fn()} />,
  );
  fireEvent.click(await screen.findByRole('button', { name: 'Нет, спасибо' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  view.unmount();
  render(
    <UpsellPrompt
      round={{ ...win, id: 'other' }}
      onOpen={vi.fn()}
      onBalance={vi.fn()}
    />,
  );
  expect(api.offer).toHaveBeenCalledOnce();
  expect(api.decide).toHaveBeenCalledWith('offer', expect.any(String), false);
});
it('upsell never requests an offer after a loss', () => {
  render(<UpsellPrompt round={loss} onOpen={vi.fn()} onBalance={vi.fn()} />);
  expect(api.offer).not.toHaveBeenCalled();
});
it('upsell automatically closes at server-provided timeout', async () => {
  vi.useFakeTimers();
  render(<UpsellPrompt round={win} onOpen={vi.fn()} onBalance={vi.fn()} />);
  await act(async () => {});
  expect(screen.getByRole('dialog')).toBeVisible();
  await act(() => vi.advanceTimersByTimeAsync(10000));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
it('repeat button stays on result and is disabled while starting', () => {
  const repeat = vi.fn();
  const view = render(
    <ResultScreen
      round={win}
      balance={balance}
      playAgain={vi.fn()}
      paused
      repeat={repeat}
    />,
  );
  fireEvent.click(screen.getByRole('button', { name: 'Повторить ставку' }));
  expect(repeat).toHaveBeenCalledOnce();
  view.rerender(
    <ResultScreen
      round={win}
      balance={balance}
      playAgain={vi.fn()}
      paused
      repeat={repeat}
      repeating
    />,
  );
  expect(screen.getByRole('button', { name: 'Запускаем…' })).toBeDisabled();
});
it('recovery discards metadata older than one hour', () => {
  saveSession({ roundId: win.id });
  const saved = JSON.parse(localStorage.getItem('skyrush.session.v1')!) as {
    roundId: string;
    savedAt: number;
  };
  localStorage.setItem(
    'skyrush.session.v1',
    JSON.stringify({ ...saved, savedAt: Date.now() - 3600001 }),
  );
  expect(readSession()).toEqual({});
  expect(localStorage.getItem('skyrush.session.v1')).toBeNull();
});
it('tournament keeps a visible retry message on transient failure', async () => {
  vi.mocked(api.tournament).mockRejectedValue(
    new ApiError('NETWORK_ERROR', 'offline'),
  );
  render(<TournamentTable />);
  expect(
    await screen.findByText('Не удалось подключиться. Попробуй ещё раз.'),
  ).toBeVisible();
});

it('result idle uses the theme-screen callback when a separate entry exists', async () => {
  vi.useFakeTimers();
  const play = vi.fn(),
    idle = vi.fn();
  render(
    <ResultScreen
      round={loss}
      balance={balance}
      playAgain={play}
      onIdle={idle}
      paused={false}
    />,
  );
  await act(() => vi.advanceTimersByTimeAsync(10000));
  expect(idle).toHaveBeenCalledOnce();
  expect(play).not.toHaveBeenCalled();
});
it('admin numeric controls stay numeric while cleared and refilled', async () => {
  render(<AdminPanel onSaved={vi.fn()} />);
  const input = await screen.findByLabelText('points.pointsPerLevel');
  fireEvent.change(input, { target: { value: '' } });
  expect(input).toHaveAttribute('type', 'number');
  fireEvent.change(input, { target: { value: '38' } });
  fireEvent.click(
    screen.getByRole('button', { name: 'Сохранить: Математика и награды' }),
  );
  await waitFor(() =>
    expect(api.saveGame).toHaveBeenCalledWith(
      expect.objectContaining({
        configuration: expect.objectContaining({
          points: expect.objectContaining({ pointsPerLevel: 38 }) as unknown,
        }) as unknown,
      }),
    ),
  );
});
it.each([1, 2, 3])(
  'renders exactly %i real players without filling the podium',
  async (count) => {
    const real = Array.from({ length: count }, (_, i) => ({
      id: String(i),
      name: `Player ${i}`,
      points: i === 0 ? 10 : 0,
      position: i + 1,
      currentPlayer: i === 0,
    }));
    vi.mocked(api.tournament).mockResolvedValue({
      name: 'Daily',
      status: 'ACTIVE',
      remainingSeconds: 120,
      endsAt: win.completedAt!,
      entries: real,
      currentPlayer: real[0],
    });
    render(<TournamentTable />);
    await screen.findByText(/Player 0/);
    expect(screen.getAllByRole('listitem')).toHaveLength(count);
    expect(screen.getByRole('list', { name: 'Лидеры' }).children).toHaveLength(
      1,
    );
    expect(screen.queryByText(/Alice|Bob|Demo/)).not.toBeInTheDocument();
  },
);
it('a sole player with zero points is shown once without invented medal winners', async () => {
  const player = {
    id: 'solo',
    name: 'Solo',
    points: 0,
    position: 1,
    currentPlayer: true,
  };
  vi.mocked(api.tournament).mockResolvedValue({
    name: 'Daily',
    status: 'ACTIVE',
    remainingSeconds: 120,
    endsAt: win.completedAt!,
    entries: [player],
    currentPlayer: player,
  });
  render(<TournamentTable />);
  await screen.findByText(/Solo/);
  expect(screen.getAllByRole('listitem')).toHaveLength(1);
  expect(
    screen.queryByRole('list', { name: 'Лидеры' }),
  ).not.toBeInTheDocument();
});
