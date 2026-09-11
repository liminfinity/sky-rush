import { expect, type Page, type APIRequestContext } from '@playwright/test';
export async function demoLogin(page: Page) {
  await page.goto('/');
  await page.getByLabel('Логин').fill('demo');
  await page.getByLabel('Пароль', { exact: true }).fill('demo12345');
  await page.getByRole('button', { name: 'Войти', exact: true }).click();
  await expect(
    page.getByRole('button', { name: 'Выбрать GREEN', exact: true }),
  ).toBeVisible();
}
export async function write(
  request: APIRequestContext,
  method: 'put' | 'post',
  url: string,
  data: unknown,
) {
  const csrf = (await (await request.get('/api/auth/csrf')).json()) as {
    headerName: string;
    token: string;
  };
  return request[method](url, {
    data,
    headers: { [csrf.headerName]: csrf.token },
  });
}
