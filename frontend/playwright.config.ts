import { defineConfig } from '@playwright/test';
if (process.env.E2E_ISOLATED_RUN !== '1')
  throw new Error(
    'Use npm run test:e2e: browser fixtures require a disposable Docker database.',
  );
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  timeout: 120000,
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
    { name: 'firefox', use: { browserName: 'firefox' } },
    { name: 'webkit', use: { browserName: 'webkit' } },
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
});
