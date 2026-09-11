import { expect, it, vi, afterEach } from 'vitest';
import { api, ApiError, errorMessage } from '../api/client';
afterEach(() => vi.unstubAllGlobals());
it('sends only the documented start intention', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(
        JSON.stringify({ headerName: 'X-CSRF-TOKEN', token: 'token' }),
      ),
    )
    .mockResolvedValue(new Response('{}'));
  vi.stubGlobal('fetch', fetch);
  await api.start({
    requestId: 'command',
    theme: 'RED',
    betOptionId: 'DOUBLE',
  });
  expect(fetch.mock.calls[1][0]).toBe('/api/rounds');
  expect(
    JSON.parse((fetch.mock.calls[1][1] as RequestInit).body as string),
  ).toEqual({
    requestId: 'command',
    theme: 'RED',
    betOptionId: 'DOUBLE',
  });
});
it('maps insufficient balance response into a useful error', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ headerName: 'X-CSRF-TOKEN', token: 'token' }),
        ),
      )
      .mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'INSUFFICIENT_BALANCE',
            message: 'not enough',
          }),
          { status: 409 },
        ),
      ),
  );
  try {
    await api.start({ requestId: 'a', theme: 'RED', betOptionId: 'DOUBLE' });
    throw new Error('expected rejection');
  } catch (e) {
    expect(e).toBeInstanceOf(ApiError);
    expect(errorMessage(e)).toContain('Не хватает бонусов');
  }
});
it('surfaces network failures', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn<typeof globalThis.fetch>()
      .mockRejectedValue(new TypeError('network')),
  );
  await expect(api.config()).rejects.toMatchObject({ code: 'NETWORK_ERROR' });
});
it('never exposes raw backend or exception text to players', () => {
  expect(
    errorMessage(
      new ApiError('INTERNAL_ERROR', 'org.sql.Exception at RoundDTO', 500),
    ),
  ).toBe('Не удалось подключиться. Попробуй ещё раз.');
  expect(
    errorMessage(new ApiError('USERNAME_TAKEN', 'database detail', 409)),
  ).toBe('Этот логин уже занят');
  expect(
    errorMessage(new ApiError('LOGIN_FAILED', 'security detail', 401)),
  ).toBe('Неверный логин или пароль');
  expect(errorMessage(new ApiError('UNKNOWN', 'secret', 401))).toBe(
    'Войди снова',
  );
});
