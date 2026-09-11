import { test, expect, type Page } from '@playwright/test';

test('real social: two accounts, presence, actual feed, daily fragment and achievements', async ({
  browser,
}, info) => {
  const base = String(process.env.E2E_BASE_URL);
  const a = await browser.newContext(),
    b = await browser.newContext();
  const alice = await a.newPage(),
    bob = await b.newPage();
  const suffix = Date.now().toString(36) + info.project.name.slice(0, 2);
  const name = 'Alice' + suffix;
  const errors: string[] = [];
  for (const page of [alice, bob])
    page.on('pageerror', (e) => errors.push(e.message));
  async function register(page: Page, displayName: string) {
    await page.goto(base);
    await page
      .getByRole('button', { name: 'Создать аккаунт', exact: true })
      .click();
    await page.getByLabel('Логин').fill(displayName);
    await page.getByLabel('Имя', { exact: true }).fill(displayName);
    await page.getByLabel('Пароль', { exact: true }).fill('password123');
    await page
      .getByRole('button', { name: 'Создать аккаунт', exact: true })
      .click();
    await expect(
      page.getByRole('button', { name: 'Выбрать GREEN', exact: true }),
    ).toBeVisible();
  }
  try {
    await register(alice, name);
    await register(bob, 'Bob' + suffix);
    await expect
      .poll(
        async () =>
          (
            (await (
              await bob.request.get(base + '/api/activity')
            ).json()) as import('../src/api/types').SocialActivity
          ).online,
      )
      .toBeGreaterThanOrEqual(2);
    const registered = (
      (await (
        await bob.request.get(base + '/api/tournament?masked=false')
      ).json()) as import('../src/api/types').Tournament
    ).entries.length;
    expect(
      (
        (await (
          await bob.request.get(base + '/api/activity')
        ).json()) as import('../src/api/types').SocialActivity
      ).online,
    ).toBeLessThanOrEqual(registered);
    await alice.getByRole('button', { name: 'Профиль', exact: true }).click();
    await expect(alice.getByText('Достижения 0 / 11')).toBeVisible();
    await alice.keyboard.press('Escape');
    await alice
      .getByRole('button', { name: 'Выбрать GREEN', exact: true })
      .click();
    await alice.getByRole('button', { name: /Ставка 40,/ }).click();
    await alice.getByRole('button', { name: 'Начать', exact: true }).click();
    let done = false,
      rounds = 0;
    while (!done && rounds < 3) {
      await expect(
        alice.getByText('Бустер ×4!', { exact: true }),
      ).toBeVisible();
      await alice.getByRole('button', { name: 'Забрать', exact: true }).click();
      await expect(alice.getByTestId('fixed-payout')).toBeVisible();
      await expect(bob.getByLabel('Сейчас в игре')).toContainText(name, {
        timeout: 9000,
      });
      const feed = (
        (await (
          await bob.request.get(base + '/api/activity')
        ).json()) as import('../src/api/types').SocialActivity
      ).events;
      expect(
        feed.some(
          (e: { displayName: string; kind: string }) =>
            e.displayName === name && e.kind === 'CASHOUT',
        ),
      ).toBe(true);
      expect(
        feed.some(
          (e: { displayName: string; kind: string }) =>
            e.displayName === name && e.kind === 'BOOSTER',
        ),
      ).toBe(true);
      await expect(
        alice.getByRole('heading', { name: 'Победа', exact: true }),
      ).toBeVisible({ timeout: 18000 });
      if (rounds === 0) {
        await expect(
          alice.getByRole('dialog', { name: 'Демо-билеты' }),
        ).toBeVisible();
        await alice.getByRole('button', { name: 'Нет, спасибо' }).click();
        await expect(
          alice.getByText('Достижение открыто', { exact: true }),
        ).toBeVisible();
      }
      rounds++;
      const d = (await (
        await alice.request.get(base + '/api/daily-challenge')
      ).json()) as import('../src/api/types').DailyChallenge;
      done = d.completed;
      expect(d.progress).toBeGreaterThan(0);
      if (!done)
        await alice
          .getByRole('button', { name: 'Повторить ставку', exact: true })
          .click();
    }
    expect(done).toBe(true);
    const daily = (await (
      await alice.request.get(base + '/api/daily-challenge')
    ).json()) as import('../src/api/types').DailyChallenge;
    expect(daily.rewardFragments).toBe(1);
    const before = (await (
      await alice.request.get(base + '/api/profile')
    ).json()) as import('../src/api/types').Profile;
    expect(before.collection.lifetimeFragments).toBe(rounds * 2 + 1);
    const bobDaily = (await (
      await bob.request.get(base + '/api/daily-challenge')
    ).json()) as import('../src/api/types').DailyChallenge;
    expect(bobDaily.progress).toBe(0);
    expect(bobDaily.rewardFragments).toBe(0);
    const completed = (await (
      await alice.request.get(base + '/api/history')
    ).json()) as import('../src/api/types').History;
    for (let i = 0; i < 4; i++)
      await alice.request.get(
        base + `/api/rounds/${completed.rounds[0].id}/state`,
      );
    expect(
      (
        (await (
          await alice.request.get(base + '/api/profile')
        ).json()) as import('../src/api/types').Profile
      ).collection.lifetimeFragments,
    ).toBe(rounds * 2 + 1);
    await alice.reload();
    await alice.getByRole('button', { name: 'Профиль', exact: true }).click();
    await expect(alice.getByLabel('Сегодня')).toContainText('Готово');
    await alice.locator('.achievements summary').click();
    await expect(
      alice.getByText('Первый полёт', { exact: true }),
    ).toBeVisible();
    await expect(alice.locator('.achievements .earned')).not.toHaveCount(0);
    for (const width of [320, 375, 768, 1024, 1440, 1920]) {
      await alice.setViewportSize({ width, height: 900 });
      expect(
        await alice
          .getByRole('dialog')
          .evaluate((e) => e.scrollWidth <= e.clientWidth),
      ).toBe(true);
      if (width === 320 || width === 1440)
        await alice.screenshot({
          path: `/tmp/skyrush-social-profile-${width}-${info.project.name}.png`,
          animations: 'disabled',
          fullPage: true,
        });
    }
    await expect
      .poll(async () => {
        const events = (
          (await (
            await bob.request.get(base + '/api/activity')
          ).json()) as import('../src/api/types').SocialActivity
        ).events;
        return events.some(
          (e: { displayName: string; kind: string }) =>
            e.displayName === name && e.kind === 'ACHIEVEMENT',
        );
      })
      .toBe(true);
    await bob.setViewportSize({ width: 320, height: 900 });
    await bob.getByLabel('Сейчас в игре').scrollIntoViewIfNeeded();
    await bob.screenshot({
      path: `/tmp/skyrush-social-feed-${info.project.name}.png`,
      fullPage: true,
      animations: 'disabled',
    });
    expect(errors).toEqual([]);
  } finally {
    await a.close();
    await b.close();
  }
});
