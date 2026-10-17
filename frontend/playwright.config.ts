import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  timeout: 60000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.BASE_URL ?? "http://localhost:8080",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "firefox",
      testMatch: "mobile.spec.ts",
      use: { ...devices["Desktop Firefox"] },
    },
    {
      name: "webkit",
      testMatch: "mobile.spec.ts",
      use: { ...devices["Desktop Safari"] },
    },
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    {
      name: "mobile",
      testMatch: "mobile.spec.ts",
      use: { ...devices["iPhone 13"], defaultBrowserType: "chromium" },
    },
  ],
});
