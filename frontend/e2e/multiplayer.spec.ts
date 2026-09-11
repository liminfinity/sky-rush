import { test, expect, type Page } from '@playwright/test';

test('two real accounts: independent gameplay and live shared scores', async ({
  browser,
}, info) => {
  const a = await browser.newContext(),
    b = await browser.newContext();
  const alice = await a.newPage(),
    bob = await b.newPage();
  const base = String(
    info.project.use.baseURL ||
      process.env.E2E_BASE_URL ||
      'http://localhost:5173',
  );
  const suffix = Date.now().toString(36) + info.project.name.slice(0, 2);
  async function register(page: Page, name: string) {
    await page.goto(base);
    await page.getByRole('button', { name: 'Создать аккаунт' }).click();
    await page.getByLabel('Логин').fill(name + suffix);
    await page.getByLabel('Имя').fill(name);
    await page.getByLabel('Пароль', { exact: true }).fill('password123');
    await page.getByRole('button', { name: 'Создать аккаунт' }).click();
    await expect(
      page.getByRole('button', { name: 'Выбрать GREEN', exact: true }),
    ).toBeVisible();
    return (await page.request.get(base + '/api/auth/me')).json() as Promise<
      import('../src/api/types').Account
    >;
  }
  async function start(page: Page, theme: string, stake: number) {
    await page
      .getByRole('button', { name: `Выбрать ${theme}`, exact: true })
      .click();
    await page
      .getByRole('button', { name: new RegExp(`Ставка ${stake},`) })
      .click();
    const response = page.waitForResponse(
      (r) => r.url().endsWith('/api/rounds') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Начать', exact: true }).click();
    return (await response).json() as Promise<import('../src/api/types').Round>;
  }
  try {
    const au = await register(alice, 'Alice');
    const beforeBob = (await (
      await alice.request.get(base + '/api/tournament?masked=false')
    ).json()) as import('../src/api/types').Tournament;
    const bu = await register(bob, 'Bob');
    expect(au.id).not.toBe(bu.id);
    const afterBob = (await (
      await bob.request.get(base + '/api/tournament?masked=false')
    ).json()) as import('../src/api/types').Tournament;
    expect(afterBob.entries.map((e: { id: string }) => e.id).sort()).toEqual(
      [...beforeBob.entries.map((e: { id: string }) => e.id), bu.id].sort(),
    );
    expect(
      (await a.cookies()).find((c) => c.name === 'JSESSIONID'),
    ).toMatchObject({ httpOnly: true, sameSite: 'Lax' });
    await alice.setViewportSize({ width: 320, height: 740 });
    const ar = await start(alice, 'GREEN', 10),
      br = await start(bob, 'RED', 20);
    expect(ar.id).not.toBe(br.id);
    expect(
      (
        (await (
          await alice.request.get(base + '/api/wallet')
        ).json()) as import('../src/api/types').Balance
      ).wallet.bonusBalance,
    ).toBe(990);
    expect(
      (
        (await (
          await bob.request.get(base + '/api/wallet')
        ).json()) as import('../src/api/types').Balance
      ).wallet.bonusBalance,
    ).toBe(980);
    expect(
      (await alice.request.get(base + `/api/rounds/${br.id}/state`)).status(),
    ).toBe(404);
    const csrf = (await (
      await alice.request.get(base + '/api/auth/csrf')
    ).json()) as { headerName: string; token: string };
    expect(
      (
        await alice.request.post(base + `/api/rounds/${br.id}/cashout`, {
          headers: { [csrf.headerName]: csrf.token },
        })
      ).status(),
    ).toBe(404);
    await expect(
      alice.getByRole('button', { name: 'Забрать', exact: true }),
    ).toBeEnabled();
    await alice.getByRole('button', { name: 'Забрать', exact: true }).click();
    await expect(alice.getByTestId('fixed-payout')).toBeVisible();
    // Bob receives Alice's later authoritative score through his own normal 450 ms round poll.
    const oldBoard = (await (
      await bob.request.get(base + '/api/tournament?masked=false')
    ).json()) as import('../src/api/types').Tournament;
    const oldAlice = oldBoard.entries.find(
      (x: { id: string }) => x.id === au.id,
    )!.points;
    const response = await bob.waitForResponse(
      async (r) => {
        if (!r.url().includes(`/api/rounds/${br.id}/state`) || !r.ok())
          return false;
        const state = (await r.json()) as import('../src/api/types').Round;
        return !!state.ranking?.entries.some(
          (x: { id: string; points: number }) =>
            x.id === au.id && x.points > oldAlice,
        );
      },
      { timeout: 2000 },
    );
    const updated = (await response.json()) as import('../src/api/types').Round;
    expect(
      updated.ranking?.entries.some(
        (x: { id: string; currentPlayer: boolean }) =>
          x.id === bu.id && x.currentPlayer,
      ),
    ).toBe(true);
    await expect(bob.getByLabel('Рейтинг полёта')).toContainText('Alice');
    const board = (await (
      await bob.request.get(base + '/api/tournament?masked=false')
    ).json()) as import('../src/api/types').Tournament;
    expect(board.entries.map((x: { points: number }) => x.points)).toEqual(
      board.entries
        .map((x: { points: number }) => x.points)
        .sort((x: number, y: number) => y - x),
    );
    await expect(alice.getByText('Победа', { exact: true })).toBeVisible({
      timeout: 20000,
    });
    await expect(
      bob.getByRole('heading', { name: 'Шар лопнул', exact: true }),
    ).toBeVisible({ timeout: 20000 });
    const ah = (await (
        await alice.request.get(base + '/api/history')
      ).json()) as import('../src/api/types').History,
      bh = (await (
        await bob.request.get(base + '/api/history')
      ).json()) as import('../src/api/types').History;
    expect(ah.rounds.map((r: { id: string }) => r.id)).toEqual([ar.id]);
    expect(bh.rounds.map((r: { id: string }) => r.id)).toEqual([br.id]);
    await bob.getByRole('button', { name: 'Рейтинг', exact: true }).click();
    await bob.getByRole('checkbox').uncheck();
    await expect(bob.getByRole('dialog')).toContainText('Alice');
    await expect(bob.locator('.tournament .current-player')).toContainText(
      'Bob',
    );
    await bob.screenshot({
      path: `/tmp/skyrush-accounts-${info.project.name}.png`,
      fullPage: true,
    });
    expect(
      (await alice.request.get(base + '/api/admin/game-config')).status(),
    ).toBe(403);
  } finally {
    await a.close();
    await b.close();
  }
});
