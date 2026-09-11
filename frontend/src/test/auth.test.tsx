import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, afterEach, it, expect, vi } from 'vitest';
import { AuthGate } from '../features/auth/AuthGate';
import { api, ApiError } from '../api/client';
import {
  accountStorage,
  saveSession,
  readSession,
} from '../features/game/storage';
vi.mock('../api/client', async (original) => ({
  ...(await original<typeof import('../api/client')>()),
  api: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() },
}));
vi.mock('../pages/HomePage', () => ({
  HomePage: ({
    onLogout,
    evaluator,
  }: {
    onLogout: () => void;
    evaluator: boolean;
  }) => (
    <section aria-label="Игра">
      {evaluator ? 'Оценщик' : 'Игрок'}
      <button type="button" onClick={onLogout}>
        Выйти
      </button>
    </section>
  ),
}));
const account = {
  id: 'alice',
  username: 'alice',
  displayName: 'Alice',
  createdAt: '2026-09-11',
  evaluator: false,
};
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.me).mockRejectedValue(
    new ApiError('UNAUTHENTICATED', 'login', 401),
  );
  vi.mocked(api.login).mockResolvedValue(account);
  vi.mocked(api.register).mockResolvedValue(account);
  vi.mocked(api.logout).mockResolvedValue({ loggedOut: true });
});
afterEach(() => accountStorage('test'));
async function form() {
  await screen.findByRole('button', { name: 'Войти' });
  fireEvent.change(screen.getByLabelText('Логин'), {
    target: { value: 'alice' },
  });
  fireEvent.change(screen.getByLabelText('Пароль'), {
    target: { value: 'password123' },
  });
}
it('login enters authenticated game and logout returns to the sky login', async () => {
  render(<AuthGate />);
  await form();
  fireEvent.click(screen.getByRole('button', { name: 'Войти' }));
  await screen.findByLabelText('Игра');
  expect(api.login).toHaveBeenCalledWith('alice', 'password123');
  fireEvent.click(screen.getByText('Выйти'));
  await screen.findByRole('button', { name: 'Войти' });
  expect(api.logout).toHaveBeenCalledOnce();
});
it('registration sends only account inputs', async () => {
  render(<AuthGate />);
  await form();
  fireEvent.click(screen.getByRole('button', { name: 'Создать аккаунт' }));
  fireEvent.change(screen.getByLabelText('Имя'), {
    target: { value: 'Alice' },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Создать аккаунт' }));
  await screen.findByLabelText('Игра');
  expect(api.register).toHaveBeenCalledWith('alice', 'Alice', 'password123');
});
it('existing session restores game and expired session returns to login', async () => {
  vi.mocked(api.me).mockResolvedValue(account);
  render(<AuthGate />);
  await screen.findByLabelText('Игра');
  window.dispatchEvent(new Event('skyrush:unauthenticated'));
  await screen.findByRole('button', { name: 'Войти' });
  expect(screen.getByRole('alert')).toHaveTextContent('Войди снова');
});
it('invalid credentials stay visible and do not enter game', async () => {
  vi.mocked(api.login).mockRejectedValue(
    new ApiError('LOGIN_FAILED', 'Неверный пароль', 401),
  );
  render(<AuthGate />);
  await form();
  fireEvent.click(screen.getByRole('button', { name: 'Войти' }));
  await screen.findByText('Неверный логин или пароль');
  expect(screen.queryByLabelText('Игра')).toBeNull();
});
it('prevents duplicate login submissions', async () => {
  let finish!: (a: typeof account) => void;
  vi.mocked(api.login).mockReturnValue(
    new Promise((resolve) => {
      finish = resolve;
    }),
  );
  render(<AuthGate />);
  await form();
  const button = screen.getByRole('button', { name: 'Войти' });
  fireEvent.click(button);
  fireEvent.click(button);
  expect(api.login).toHaveBeenCalledOnce();
  finish(account);
  await screen.findByLabelText('Игра');
});
it('recovery metadata belongs to an account', () => {
  accountStorage('alice');
  saveSession({ roundId: '11111111-1111-4111-8111-111111111111' });
  accountStorage('bob');
  expect(readSession()).toEqual({});
  accountStorage('alice');
  expect(readSession().roundId).toBe('11111111-1111-4111-8111-111111111111');
});
it('failed logout does not silently claim the session ended', async () => {
  vi.mocked(api.me).mockResolvedValue(account);
  vi.mocked(api.logout).mockRejectedValue(
    new ApiError('NETWORK_ERROR', 'offline'),
  );
  render(<AuthGate />);
  await screen.findByLabelText('Игра');
  fireEvent.click(screen.getByText('Выйти'));
  await waitFor(() => expect(screen.getByRole('alert')).toBeVisible());
  expect(screen.getByLabelText('Игра')).toBeVisible();
});
