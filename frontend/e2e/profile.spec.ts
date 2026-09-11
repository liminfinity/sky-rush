import { test, expect } from '@playwright/test';

test('profile: real fragments unlock cosmetics, records, equip and next flight', async ({
  page,
}, info) => {
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  await page.goto('/');
  await page
    .getByRole('button', { name: 'Создать аккаунт', exact: true })
    .click();
  await page
    .getByLabel('Логин')
    .fill(
      'collection_' + Date.now().toString(36) + info.project.name.slice(0, 2),
    );
  await page.getByLabel('Имя', { exact: true }).fill('Небо');
  await page.getByLabel('Пароль', { exact: true }).fill('password123');
  await page
    .getByRole('button', { name: 'Создать аккаунт', exact: true })
    .click();
  await page.getByRole('button', { name: 'Профиль', exact: true }).click();
  await page.locator('.profile-collection summary').click();
  await expect(page.getByText('0 / 5 фрагментов')).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Закрыто: Созвездия' }),
  ).toBeDisabled();
  await page.keyboard.press('Escape');
  await page
    .getByRole('button', { name: 'Выбрать GREEN', exact: true })
    .click();
  await page.getByRole('button', { name: /Ставка 10,/ }).click();
  await page.getByRole('button', { name: 'Начать', exact: true }).click();
  for (let i = 0; i < 5; i++) {
    await expect(
      page.getByRole('button', { name: 'Забрать', exact: true }),
    ).toBeEnabled();
    await page.getByRole('button', { name: 'Забрать', exact: true }).click();
    await expect(page.getByTestId('fixed-payout')).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Победа', exact: true }),
    ).toBeVisible({ timeout: 18000 });
    await expect(page.locator('.win-comparison')).toBeVisible();
    const completedProfile = (await (
      await page.request.get('/api/profile')
    ).json()) as import('../src/api/types').Profile;
    const history = (await (
      await page.request.get('/api/history')
    ).json()) as import('../src/api/types').History;
    const unlocked = completedProfile.collection.items.filter(
      (item) => item.unlockedRoundId === history.rounds[0].id,
    );
    if (unlocked.length) {
      for (const item of unlocked)
        await expect(page.getByLabel('Новая награда')).toContainText(item.name);
      await page.screenshot({
        path: `/tmp/skyrush-profile-unlock-${i}-${info.project.name}.png`,
        fullPage: true,
        animations: 'disabled',
      });
    }
    if (i < 4)
      await page
        .getByRole('button', { name: 'Повторить ставку', exact: true })
        .click();
  }
  await page.getByRole('button', { name: 'Профиль', exact: true }).click();
  await page.locator('.profile-collection summary').click();
  const daily = (await (
    await page.request.get('/api/daily-challenge')
  ).json()) as import('../src/api/types').DailyChallenge;
  expect([0, 1]).toContain(daily.rewardFragments);
  await expect(
    page.getByText(`${10 + daily.rewardFragments} / 15 фрагментов`),
  ).toBeVisible();
  await page.getByText('Все рекорды').click();
  const state = (await (
    await page.request.get('/api/profile')
  ).json()) as import('../src/api/types').Profile;
  expect(state.records).toMatchObject({
    totalRounds: 5,
    successfulCashouts: 5,
    losses: 0,
    totalFragments: 10 + daily.rewardFragments,
    currentWinStreak: 5,
    bestWinStreak: 5,
  });
  expect(state.fragmentBalance).toBe(daily.rewardFragments);
  for (const width of [320, 375, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    expect(
      await page
        .getByRole('dialog')
        .evaluate((e) => e.scrollWidth <= e.clientWidth),
    ).toBe(true);
    await page
      .getByRole('button', { name: 'Надеть: Созвездия', exact: true })
      .scrollIntoViewIfNeeded();
    await expect(
      page.getByRole('button', { name: 'Надеть: Созвездия', exact: true }),
    ).toBeInViewport();
    if (width === 320 || width === 1440)
      await page.screenshot({
        path: `/tmp/skyrush-profile-${width}-${info.project.name}.png`,
        fullPage: true,
        animations: 'disabled',
      });
  }
  await page.getByRole('button', { name: 'Надеть: Созвездия' }).click();
  await expect(
    page.getByRole('button', { name: 'Выбрано: Созвездия' }),
  ).toBeDisabled();
  await expect(
    page.getByRole('button', { name: 'Надеть: Бронза' }),
  ).toBeEnabled();
  await page.getByRole('button', { name: 'Надеть: Бронза' }).click();
  await expect(
    page.getByRole('button', { name: 'Выбрано: Бронза' }),
  ).toBeDisabled();
  await page.keyboard.press('Escape');
  await page
    .getByRole('button', { name: 'Повторить ставку', exact: true })
    .click();
  await expect(page.locator('.flight-balloon svg')).toHaveAttribute(
    'data-skin',
    'constellations',
  );
  await expect(page.locator('.player-identity [data-frame]')).toHaveAttribute(
    'data-frame',
    'bronze',
  );
  await expect(
    page.locator('.live-ranking .current-player [data-frame]'),
  ).toHaveAttribute('data-frame', 'bronze');
  await page.setViewportSize({ width: 320, height: 740 });
  await page.screenshot({
    path: `/tmp/skyrush-profile-flight-${info.project.name}.png`,
    fullPage: true,
    animations: 'disabled',
  });
  await page.reload();
  await expect(page.locator('.flight-balloon svg')).toHaveAttribute(
    'data-skin',
    'constellations',
  );
  await expect(page.locator('.player-identity [data-frame]')).toHaveAttribute(
    'data-frame',
    'bronze',
  );
  await page.getByRole('button', { name: 'Рейтинг', exact: true }).click();
  await expect(
    page.locator('.tournament .current-player [data-frame]'),
  ).toHaveAttribute('data-frame', 'bronze');
  expect(errors).toEqual([]);
});
