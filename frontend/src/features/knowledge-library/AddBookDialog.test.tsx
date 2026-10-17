import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AddBookDialog } from "./AddBookDialog";
import { send } from "../../shared/api";
vi.mock("../../shared/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../../shared/api")>()),
  send: vi.fn(),
}));
describe("manual registration", () => {
  beforeEach(() => {
    vi.mocked(send).mockReset();
  });
  async function fill() {
    const user = userEvent.setup();
    render(<AddBookDialog onClose={() => {}} onSaved={() => {}} />);
    await user.click(screen.getByRole("button", { name: "Enter manually" }));
    await user.type(
      screen.getByLabelText("Title", { exact: true }),
      "A personal book",
    );
    await user.click(
      screen.getByRole("button", { name: "Confirm this edition" }),
    );
    return user;
  }
  it("retains entered information after a failed save and allows retry", async () => {
    vi.mocked(send).mockRejectedValueOnce(new Error("Connection lost"));
    const user = await fill();
    const review = screen.getByLabelText(/Your reflections/);
    await user.type(review, "A thought worth keeping");
    await user.click(screen.getByRole("button", { name: "Add to library" }));
    await screen.findByRole("alert");
    expect(review).toHaveValue("A thought worth keeping");
    expect(screen.getAllByText("A personal book")[0]).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Add to library" }),
    ).toBeEnabled();
  });
  it("sends the local calendar date and optional review without inventing metadata", async () => {
    vi.mocked(send).mockRejectedValue(new Error("Keep form open"));
    const user = await fill();
    await user.click(screen.getByRole("button", { name: "Add to library" }));
    await screen.findByRole("alert");
    const payload = vi.mocked(send).mock.calls[0][2] as {
      reading: { localDate: string; review: null; completionDate: null };
      metadata: { authors: unknown[]; title: string };
    };
    expect(payload.reading.localDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(payload.reading.review).toBeNull();
    expect(payload.reading.completionDate).toBeNull();
    expect(payload.metadata.authors).toEqual([]);
    expect(payload.metadata.title).toBe("A personal book");
  });
});
