import { demoLogin, write } from './auth-helpers';
import { test, expect, type Page } from '@playwright/test';
async function widths(page: Page) {
  for (const width of [320, 375, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
    if (await page.getByRole('dialog').count())
      await expect(
        page.getByRole('button', { name: 'Закрыть окно' }),
      ).toBeInViewport();
  }
  await page.setViewportSize({ width: 375, height: 900 });
}
test.beforeEach(async ({ page }) => {
  await demoLogin(page);
});
test('real extensions: tournament, admin snapshots, proof, ticket purchase, repeat and recovery', async ({
  page,
}, info) => {
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  await page.goto('/');
  await expect(
    page.getByRole('button', { name: 'Выбрать RED', exact: true }),
  ).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await widths(page);
  await page.screenshot({
    path: `/tmp/skyrush-theme-${info.project.name}.png`,
    fullPage: true,
  });
  await page.getByRole('button', { name: 'Выбрать RED', exact: true }).click();
  await page.getByRole('button', { name: /Рейтинг/ }).click();
  await expect(page.getByText(/До конца/)).toBeVisible();
  await widths(page);
  await page.screenshot({
    path: `/tmp/skyrush-tournament-${info.project.name}.png`,
    fullPage: true,
  });
  await expect(page.locator('.tournament .current-player')).toContainText('Ты');
  await page.keyboard.press('Escape');
  const original = (await (
    await page.request.get('/api/admin/game-config')
  ).json()) as import('../src/api/types').ConfigSnapshot<
    import('../src/api/types').GameConfiguration
  >;
  try {
    await page.getByLabel('Ещё', { exact: true }).click();
    await page
      .getByRole('button', { name: 'Настройки игры', exact: true })
      .click();
    await expect(
      page.getByLabel('points.pointsPerLevel', { exact: true }),
    ).toBeVisible();
    await widths(page);
    await page.screenshot({
      path: `/tmp/skyrush-admin-${info.project.name}.png`,
      fullPage: true,
    });
    await page.getByLabel('points.pointsPerLevel', { exact: true }).fill('37');
    await page
      .getByRole('button', { name: 'Сохранить: Математика и награды' })
      .click();
    await expect(page.getByText(/Сохранено/)).toBeVisible();
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: /Ставка 30,/ }).click();
    const startResponse = page.waitForResponse(
      (r) => r.url().endsWith('/api/rounds') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: /Начать/ }).click();
    const round = (await (
      await startResponse
    ).json()) as import('../src/api/types').Round;
    expect(round.pointsRules.pointsPerLevel).toBe(37);
    expect(round.integrity?.reveal).toBeNull();
    expect(round.integrity?.commitment).toHaveLength(64);
    await expect(page.getByLabel('Рейтинг полёта')).toContainText('Ты');
    await expect(page.getByText(/Бустер ×3!/)).toBeVisible();
    await page.getByRole('button', { name: 'Забрать', exact: true }).click();
    await expect(page.getByTestId('fixed-payout')).toBeVisible();
    await widths(page);
    const paid = (await (
      await page.request.get(`/api/rounds/${round.id}/state`)
    ).json()) as import('../src/api/types').Round;
    expect(paid.state).toBe('CASHED_OUT');
    const current = (await (
      await page.request.get('/api/admin/game-config')
    ).json()) as import('../src/api/types').ConfigSnapshot<
      import('../src/api/types').GameConfiguration
    >;
    current.configuration.points.pointsPerLevel = 38;
    expect(
      (
        await write(page.request, 'put', '/api/admin/game-config', current)
      ).ok(),
    ).toBeTruthy();
    expect(
      (
        (await (
          await page.request.get(`/api/rounds/${round.id}/state`)
        ).json()) as import('../src/api/types').Round
      ).pointsRules.pointsPerLevel,
    ).toBe(37);
    await expect(page.getByRole('dialog', { name: 'Демо-билеты' })).toBeVisible(
      { timeout: 20000 },
    );
    await widths(page);
    await page.screenshot({
      path: `/tmp/skyrush-upsell-${info.project.name}.png`,
      fullPage: true,
    });
    const before = (await (
      await page.request.get('/api/wallet')
    ).json()) as import('../src/api/types').Balance;
    const purchaseResponse = page.waitForResponse(
      (r) => r.url().includes('/decision') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Купить' }).click();
    const purchase = (await (
      await purchaseResponse
    ).json()) as import('../src/api/types').Tickets & {
      offer: NonNullable<import('../src/api/types').Tickets['offer']>;
    };
    expect(purchase.offer.status).toBe('PURCHASED');
    expect(purchase.tickets).toBeGreaterThanOrEqual(purchase.offer.quantity);
    const after = (await (
      await page.request.get('/api/wallet')
    ).json()) as import('../src/api/types').Balance;
    expect(after.wallet.bonusBalance).toBeCloseTo(
      before.wallet.bonusBalance - purchase.offer.total,
      2,
    );
    await widths(page);
    await page.screenshot({
      path: `/tmp/skyrush-result-${info.project.name}.png`,
      fullPage: true,
    });
    await page.getByLabel('Ещё', { exact: true }).click();
    await page
      .getByRole('button', { name: 'Проверка полёта', exact: true })
      .click();
    await page.getByText('Проверить исход', { exact: true }).click();
    await page.getByRole('button', { name: 'Проверить' }).click();
    await expect(page.getByText('Исход не изменён.')).toBeVisible();
    await page.keyboard.press('Escape');
    const completed = (await (
      await page.request.get(`/api/rounds/${round.id}/state`)
    ).json()) as import('../src/api/types').Round;
    expect(completed.payout).toBe(paid.payout);
    const repeatResponse = page.waitForResponse(
      (r) => r.url().endsWith('/api/rounds') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Повторить ставку' }).click();
    const repeat = (await (
      await repeatResponse
    ).json()) as import('../src/api/types').Round;
    expect(repeat.id).not.toBe(round.id);
    expect(repeat.theme).toBe('RED');
    expect(repeat.betOptionId).toBe('TRIPLE');
    expect(repeat.pointsRules.pointsPerLevel).toBe(38);
    // Server discovery also works when browser-local recovery data has gone.
    await page.evaluate(() =>
      Object.keys(localStorage)
        .filter((k) => k.startsWith('skyrush.session.v1'))
        .forEach((k) => localStorage.removeItem(k)),
    );
    await page.reload();
    await expect(page.getByLabel('Полёт воздушного шара')).toBeVisible();
    await page.getByRole('button', { name: 'Забрать', exact: true }).click();
    await expect(page.getByText('Победа')).toBeVisible({ timeout: 20000 });
    await expect(
      page.getByRole('dialog', { name: 'Демо-билеты' }),
    ).not.toBeVisible();
    await page.getByRole('button', { name: /Играть снова/ }).click();
    // Genuine affordability UI using an expensive server-administered stake, no mocked wallet.
    const expensive = (await (
      await page.request.get('/api/admin/game-config')
    ).json()) as import('../src/api/types').ConfigSnapshot<
      import('../src/api/types').GameConfiguration
    >;
    expensive.configuration.betOptions[0].stake = 1000000;
    await write(page.request, 'put', '/api/admin/game-config', expensive);
    await page.reload();
    await page
      .getByRole('button', { name: 'Выбрать RED', exact: true })
      .click();
    await expect(
      page.getByRole('button', { name: /Ставка 1.*000.*000/ }),
    ).toBeDisabled();
    expect(errors).toEqual([]);
  } finally {
    const latest = (await (
      await page.request.get('/api/admin/game-config')
    ).json()) as import('../src/api/types').ConfigSnapshot<
      import('../src/api/types').GameConfiguration
    >;
    await write(page.request, 'put', '/api/admin/game-config', {
      version: latest.version,
      configuration: original.configuration,
    });
  }
});
