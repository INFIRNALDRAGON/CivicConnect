import { defineConfig } from '@playwright/test';

const external = process.env.E2E_BASE_URL;
const localUrl = 'http://localhost:5174';

export default defineConfig({
  testDir: './e2e',
  outputDir: './test-results/artifacts',
  timeout: 60000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }], ['junit', { outputFile: 'test-results/e2e.xml' }]]
    : [['list']],
  use: {
    baseURL: external ?? localUrl,
    viewport: { width: 1280, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: process.env.PW_CHROMIUM_PATH ? { executablePath: process.env.PW_CHROMIUM_PATH } : {}
  },
  webServer: external
    ? undefined
    : {
        command: 'npm run dev -- --port 5174 --strictPort',
        env: { VITE_USE_MOCK: 'true' },
        url: localUrl,
        reuseExistingServer: !process.env.CI,
        timeout: 90000
      }
});
