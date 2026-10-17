import { test, expect, type APIRequestContext } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
const password = "a long personal passphrase";
const mailpit = process.env.MAILPIT_URL ?? "http://localhost:8025";
async function emailToken(
  request: APIRequestContext,
  email: string,
  subject: string,
) {
  let token = "";
  await expect
    .poll(
      async () => {
        const response = await request.get(mailpit + "/api/v1/messages");
        const data = await response.json();
        const message = data.messages?.find(
          (m: { Subject: string; To: { Address: string }[] }) =>
            m.Subject.includes(subject) &&
            m.To.some((t) => t.Address === email),
        );
        if (!message) return false;
        const detail = await (
          await request.get(mailpit + "/api/v1/message/" + message.ID)
        ).json();
        token = detail.Text.match(/#token=([^\s]+)/)?.[1] ?? "";
        return !!token;
      },
      { timeout: 20000 },
    )
    .toBe(true);
  return token;
}
test("verified account, books, rereads, covers, isolation and recovery", async ({
  page,
  request,
  browser,
  baseURL,
}) => {
  page.on("pageerror", (error) => {
    throw error;
  });
  const email = `reader-${Date.now()}@example.test`;
  await page.goto("/register");
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel(/^Password/).fill(password);
  await page.getByRole("button", { name: "Create your library" }).click();
  await expect(page.getByRole("status")).toContainText("verification link");
  const verify = await emailToken(request, email, "Verify");
  await page.goto("/verify-email#token=" + verify);
  await page.getByRole("button", { name: "Verify email" }).click();
  await expect(page.getByRole("status")).toContainText("Email verified");
  await page.getByRole("link", { name: "Back to sign in" }).click();
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel(/^Password/).fill(password);
  await page.getByRole("button", { name: "Step inside" }).click();
  await expect(
    page.getByRole("heading", { name: "Your knowledge library." }),
  ).toBeVisible();
  await expect(
    page.getByText("A little empty. Full of possibility."),
  ).toBeVisible();
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await page
    .getByRole("button", { name: "Open Add Book using the computer" })
    .click();
  await page.getByLabel("Book title", { exact: true }).fill("garden");
  await page
    .getByRole("button", { name: /The Test Garden.*Select edition/ })
    .click();
  await expect(page.getByLabel("Title", { exact: true })).toHaveValue(
    "The Test Garden",
  );
  await expect(
    page.getByText("A garden of ideas.", { exact: false }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Confirm this edition" }).click();
  await page
    .getByLabel("Your reflections", { exact: false })
    .fill("First thought.\n\nSecond thought.");
  await page
    .getByRole("button", { name: "Add to library", exact: true })
    .click();
  const spine = page.getByRole("button", {
    name: "Open The Test Garden by Mira Green",
    exact: true,
  });
  await expect(spine).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(spine).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/library-mobile.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.reload();
  await spine.focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText("First thought.\n\nSecond thought.", { exact: true }),
  ).toBeVisible();
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await page.getByRole("button", { name: /Edit reading completed/ }).click();
  await page
    .getByLabel("Your reflections", { exact: false })
    .fill("A revised reflection.");
  await page.getByRole("button", { name: "Save reading", exact: true }).click();
  await expect(
    page.getByText("A revised reflection.", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Add reread" }).click();
  await page
    .getByLabel("Your reflections", { exact: false })
    .fill("Read it again.");
  await page.getByRole("button", { name: "Save reading", exact: true }).click();
  await expect(page.getByText("Read it again.", { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Customize cover", exact: true })
    .click();
  // Generate a real PNG rather than relying on a remote cover.
  const png = await page.evaluate(() => {
    const c = document.createElement("canvas");
    c.width = 20;
    c.height = 30;
    c.getContext("2d")!.fillRect(0, 0, 20, 30);
    return c.toDataURL().split(",")[1];
  });
  await page.locator("input[type=file]").setInputFiles({
    name: "cover.png",
    mimeType: "image/png",
    buffer: Buffer.from(png, "base64"),
  });
  await expect(page.getByText("Your upload", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Draw a cover", exact: true }).click();
  const canvas = page.locator("canvas");
  const bounds = (await canvas.boundingBox())!;
  await page.mouse.move(bounds.x + 30, bounds.y + 30);
  await page.mouse.down();
  await page.mouse.move(bounds.x + 90, bounds.y + 110);
  await page.mouse.up();
  await page.getByRole("button", { name: "Use this drawing" }).click();
  await expect(page.getByText("Your drawing", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page.reload();
  await spine.click();
  await page
    .getByRole("button", { name: "Customize cover", exact: true })
    .click();
  await expect(page.getByText("Your drawing", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Remove drawing" }).click();
  await expect(page.getByText("Your drawing", { exact: true })).toHaveCount(0);
  await expect(page.getByText("Your upload", { exact: true })).toBeVisible();
  // Anonymous requests cannot read private library data.
  const stranger = await browser.newContext({ baseURL });
  const unauthorized = await stranger.request.get("/api/v1/library/books");
  expect(unauthorized.status()).toBe(401);
  await stranger.close();
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page.getByRole("button", { name: "Add a book", exact: true }).click();
  await page.getByRole("button", { name: "Enter manually" }).click();
  await page.getByLabel("Title", { exact: true }).fill("My handwritten book");
  await page.getByRole("button", { name: "Confirm this edition" }).click();
  await page
    .getByRole("button", { name: "Add to library", exact: true })
    .click();
  await expect(
    page.getByRole("button", {
      name: "Open My handwritten book by unknown author",
      exact: true,
    }),
  ).toBeVisible();
  await spine.click();
  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "Delete from library" }).click();
  await expect(spine).toHaveCount(0);
  await page.screenshot({
    path: "test-results/library-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Sign out" }).click();
  await page.getByRole("link", { name: "Forgot your password?" }).click();
  await expect(
    page.getByRole("heading", { name: "Let’s get you back in." }),
  ).toBeVisible();
  await page.getByLabel("Email address").fill(email);
  await page.getByRole("button", { name: "Send reset link" }).click();
  await expect(page.getByRole("status")).toContainText("reset link");
  const reset = await emailToken(request, email, "Reset");
  await page.goto("/reset-password#token=" + reset);
  await page.getByLabel(/^Password/).fill("another long personal passphrase");
  await page.getByRole("button", { name: "Save new password" }).click();
  await expect(page.getByRole("status")).toContainText("Password updated");
});
