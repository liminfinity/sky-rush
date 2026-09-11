import type {
  Balance,
  History,
  PublicConfig,
  Round,
  StartRequest,
  User,
  Tournament,
  ConfigSnapshot,
  GameConfiguration,
  PrototypeConfiguration,
  Tickets,
} from './types';

export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || '/api'
).replace(/\/+$/, '');
export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status = 0,
  ) {
    super(message);
  }
}
const messages: Record<string, string> = {
  COSMETIC_LOCKED: 'Этот предмет ещё закрыт',
  COSMETIC_NOT_FOUND: 'Предмет не найден',
  INSUFFICIENT_BALANCE: 'Не хватает бонусов',
  ROUND_IN_PROGRESS: 'Уже идёт полёт. Вернись к нему.',
  LEVEL_ONE_REQUIRED: 'Забрать можно после первого уровня',
  ALREADY_CASHED_OUT: 'Ты уже забрал выигрыш',
  ROUND_CRASHED: 'Шар уже лопнул',
  ROUND_NOT_FOUND: 'Полёт не найден. Выбери новую ставку.',
  CONFIG_INVALID: 'Игра пока недоступна',
  GAME_INACTIVE: 'Игра на паузе',
  DEMO_DISABLED: 'Вход для жюри недоступен',
  INVALID_BET: 'Ставка недоступна. Выбери другую.',
  INVALID_THEME: 'Выбери шар заново',
  REQUEST_ID_CONFLICT: 'Не удалось начать. Обнови страницу.',
  INVALID_REQUEST: 'Проверь заполненные поля',
  USERNAME_TAKEN: 'Этот логин уже занят',
  LOGIN_FAILED: 'Неверный логин или пароль',
  UNAUTHENTICATED: 'Войди снова',
  CSRF_UNAVAILABLE: 'Обнови страницу и попробуй снова',
  OFFER_CLOSED: 'Предложение уже закрыто',
  OFFER_NOT_FOUND: 'Предложение недоступно',
  OFFER_SESSION: 'Предложение недоступно',
};
const connectionMessage = 'Не удалось подключиться. Попробуй ещё раз.';
export const errorMessage = (error: unknown): string => {
  if (!(error instanceof ApiError)) return connectionMessage;
  if (messages[error.code]) return messages[error.code];
  if (error.status === 401) return 'Войди снова';
  if (error.status === 403) return 'Обнови страницу и попробуй снова';
  if (error.status === 400) return 'Проверь заполненные поля';
  return connectionMessage;
};
// Evaluator forms need precise field validation; player screens never show raw API messages.
export const adminErrorMessage = (error: unknown): string =>
  error instanceof ApiError &&
  ['CONFIG_VALIDATION', 'CONFIG_CONFLICT', 'CONFIG_WRITE_FAILED'].includes(
    error.code,
  )
    ? error.message
    : errorMessage(error);

export async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const controller = new AbortController();
  const abort = () => controller.abort();
  init.signal?.addEventListener('abort', abort, { once: true });
  if (init.signal?.aborted) controller.abort();
  const timeout = setTimeout(abort, 8000);
  try {
    const headers = new Headers(init.headers);
    if (init.body) headers.set('Content-Type', 'application/json');
    if (
      init.method &&
      !['GET', 'HEAD', 'OPTIONS'].includes(init.method.toUpperCase())
    ) {
      const csrfResponse = await fetch(`${API_BASE_URL}/auth/csrf`, {
        signal: controller.signal,
        credentials: 'same-origin',
      });
      if (!csrfResponse.ok)
        throw new ApiError(
          'CSRF_UNAVAILABLE',
          'Не удалось проверить сессию',
          csrfResponse.status,
        );
      const csrf = (await csrfResponse.json()) as {
        headerName: string;
        token: string;
      };
      headers.set(csrf.headerName, csrf.token);
    }
    const response = await fetch(
      `${API_BASE_URL}/${path.replace(/^\/+/, '')}`,
      {
        ...init,
        signal: controller.signal,
        credentials: 'same-origin',
        headers,
      },
    );
    if (!response.ok) {
      const body = (await response.json().catch(() => null)) as {
        code?: string;
        message?: string;
      } | null;
      if (response.status === 401 && !path.startsWith('auth/'))
        window.dispatchEvent(new Event('skyrush:unauthenticated'));
      throw new ApiError(
        body?.code || 'HTTP_ERROR',
        body?.message || `Сервер временно недоступен (${response.status}).`,
        response.status,
      );
    }
    return (await response.json()) as T;
  } catch (error) {
    if (init.signal?.aborted) throw new DOMException('Aborted', 'AbortError');
    if (error instanceof ApiError) throw error;
    throw new ApiError(
      'NETWORK_ERROR',
      'Нет ответа от сервера. Проверьте соединение и повторите попытку.',
    );
  } finally {
    clearTimeout(timeout);
    init.signal?.removeEventListener('abort', abort);
  }
}
export const api = {
  achievements: (signal?: AbortSignal) =>
    request<import('./types').Achievement[]>('achievements', { signal }),
  daily: (signal?: AbortSignal) =>
    request<import('./types').DailyChallenge>('daily-challenge', { signal }),
  activity: (signal?: AbortSignal) =>
    request<import('./types').SocialActivity>('activity', { signal }),
  heartbeat: (signal?: AbortSignal) =>
    request<import('./types').SocialActivity>('presence/heartbeat', {
      method: 'POST',
      body: '{}',
      signal,
    }),
  profile: (signal?: AbortSignal) =>
    request<import('./types').Profile>('profile', { signal }),
  collection: (signal?: AbortSignal) =>
    request<import('./types').Collection>('profile/collection', { signal }),
  equip: (cosmeticId: string, signal?: AbortSignal) =>
    request<import('./types').Collection>('profile/cosmetics/equip', {
      method: 'POST',
      body: JSON.stringify({ cosmeticId }),
      signal,
    }),
  me: (signal?: AbortSignal) =>
    request<import('./types').Account>('auth/me', { signal }),
  login: (username: string, password: string) =>
    request<import('./types').Account>('auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  register: (username: string, displayName: string, password: string) =>
    request<import('./types').Account>('auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, displayName, password }),
    }),
  logout: () =>
    request<{ loggedOut: boolean }>('auth/logout', { method: 'POST' }),
  active: (signal?: AbortSignal) =>
    request<{ round: Round | null }>('rounds/active', { signal }),
  tournament: (masked = true, signal?: AbortSignal) =>
    request<Tournament>(`tournament?masked=${masked}`, { signal }),
  adminGame: (signal?: AbortSignal) =>
    request<ConfigSnapshot<GameConfiguration>>('admin/game-config', { signal }),
  adminPrototype: (signal?: AbortSignal) =>
    request<ConfigSnapshot<PrototypeConfiguration>>('admin/prototype-config', {
      signal,
    }),
  saveGame: (body: ConfigSnapshot<GameConfiguration>) =>
    request<ConfigSnapshot<GameConfiguration>>('admin/game-config', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  savePrototype: (body: ConfigSnapshot<PrototypeConfiguration>) =>
    request<ConfigSnapshot<PrototypeConfiguration>>('admin/prototype-config', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  offer: (sessionId: string, roundId: string, signal?: AbortSignal) =>
    request<Tickets>('upsell/offers', {
      method: 'POST',
      body: JSON.stringify({ sessionId, roundId }),
      signal,
    }),
  decide: (id: string, sessionId: string, accept: boolean) =>
    request<Tickets>(`upsell/offers/${encodeURIComponent(id)}/decision`, {
      method: 'POST',
      body: JSON.stringify({ sessionId, accept }),
    }),
  tickets: (signal?: AbortSignal) =>
    request<Tickets>('upsell/tickets', { signal }),
  user: (signal?: AbortSignal) => request<User>('auth/me', { signal }),
  balance: (signal?: AbortSignal) => request<Balance>('wallet', { signal }),
  config: (signal?: AbortSignal) =>
    request<PublicConfig>('game/config/public', { signal }),
  start: (body: StartRequest, signal?: AbortSignal) =>
    request<Round>('rounds', {
      method: 'POST',
      body: JSON.stringify(body),
      signal,
    }),
  state: (id: string, signal?: AbortSignal) =>
    request<Round>(`rounds/${encodeURIComponent(id)}/state`, { signal }),
  cashout: (id: string, signal?: AbortSignal) =>
    request<Round>(`rounds/${encodeURIComponent(id)}/cashout`, {
      method: 'POST',
      signal,
    }),
  history: (offset = 0, signal?: AbortSignal) =>
    request<History>(`history?limit=10&offset=${offset}`, { signal }),
};
