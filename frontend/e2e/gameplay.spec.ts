import { demoLogin } from './auth-helpers';
import { test, expect } from '@playwright/test';
// Uses a running frontend and REAL backend. No route mocks or fabricated results.
test.beforeEach(async ({ page }) => {
  await demoLogin(page);
});
test('real backend: themes, flight, recovery, cashout, loss, history and idle return', async ({
  page,
}) => {
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  await page.goto('/');
  await page
    .getByRole('button', { name: 'Выбрать GREEN', exact: true })
    .click();
  await expect(page.getByRole('button', { name: /Начать/ })).toBeVisible();
  for (const width of [320, 375, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({ path: '/tmp/skyrush-desktop.png', fullPage: true });
  await page.getByRole('button', { name: /Правила/ }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  for (const width of [320, 375, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: /RED/ }).click();
  await expect(page.getByLabel('12 уровней полёта')).toBeVisible();
  await page.getByRole('button', { name: /Ставка 30,/ }).click();
  await page.getByRole('button', { name: /Начать/ }).click();
  await expect(page.getByLabel('Полёт воздушного шара')).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('Полёт воздушного шара')).toBeVisible();
  await expect(page.getByText(/Бустер ×3!/)).toBeVisible({ timeout: 45000 });
  await page.setViewportSize({ width: 320, height: 740 });
  const button = page.getByRole('button', { name: 'Забрать', exact: true });
  await expect(button).toBeInViewport();
  await page.screenshot({
    path: '/tmp/skyrush-mobile-flight.png',
    fullPage: true,
  });
  await button.click();
  await expect(page.getByTestId('fixed-payout')).toBeVisible();
  const payout = await page.getByTestId('fixed-payout').textContent();
  await expect(page.getByText(/Могли бы забрать больше/)).toBeVisible();
  await expect(page.getByText('Победа')).toBeVisible({ timeout: 50000 });
  await expect(page.getByRole('dialog', { name: 'Демо-билеты' })).toBeVisible();
  await page.getByRole('button', { name: 'Нет, спасибо' }).click();
  await page.getByRole('button', { name: /Играть снова/ }).click();
  await expect(page.getByRole('button', { name: /RED/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await page.getByRole('button', { name: /GREEN/ }).click();
  await expect(page.getByLabel('9 уровней полёта')).toBeVisible();
  await page.getByRole('button', { name: /Ставка 10,/ }).click();
  await page.getByRole('button', { name: /Начать/ }).click();
  await expect(
    page.getByRole('heading', { name: 'Шар лопнул', exact: true }),
  ).toBeVisible({ timeout: 50000 });
  await page.screenshot({
    path: '/tmp/skyrush-mobile-result.png',
    fullPage: true,
  });
  await expect(
    page.getByRole('button', { name: 'Выбрать GREEN', exact: true }),
  ).toBeVisible({ timeout: 13000 });
  await page
    .getByRole('button', { name: 'Выбрать GREEN', exact: true })
    .click();
  await expect(page.getByRole('button', { name: /GREEN/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await page.getByRole('button', { name: /История/ }).click();
  await expect(
    page.getByRole('dialog').getByText('Победа').first(),
  ).toBeVisible();
  await expect(
    page.getByRole('dialog').getByText('Проигрыш').first(),
  ).toBeVisible();
  for (const width of [320, 375, 768, 1024, 1440, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  expect(payout).toBeTruthy();
  expect(errors).toEqual([]);
});
