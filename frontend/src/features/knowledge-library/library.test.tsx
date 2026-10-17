import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { packShelves, localDate } from "./utils";
import { BookCover } from "./BookCover";
import { ReadingFields } from "./ReadingFields";
import { MetadataFields } from "./MetadataFields";
import { emptyMetadata, type LibraryBook } from "./types";
describe("shelf layout", () => {
  const book = (id: string, width: number) =>
    ({ id, spineWidth: width }) as LibraryBook;
  it("starts with three empty shelves", () =>
    expect(packShelves([], 600)).toEqual([[], [], []]));
  it("packs in stable order and moves whole books onto subsequent shelves", () => {
    const books = Array.from({ length: 1000 }, (_, i) => book(String(i), 40));
    const shelves = packShelves(books, 150);
    expect(shelves.flat()).toEqual(books);
    for (const shelf of shelves)
      expect(
        shelf.reduce((n, b) => n + b.spineWidth + 5, 0),
      ).toBeLessThanOrEqual(150);
  });
  it("reflows deterministically at mobile widths", () => {
    const books = [book("a", 50), book("b", 50), book("c", 50)];
    expect(packShelves(books, 110)[0]).toHaveLength(2);
    expect(packShelves(books, 60)[0]).toHaveLength(1);
  });
});
describe("reading and metadata", () => {
  it("uses calendar fields rather than a UTC ISO date", () =>
    expect(localDate(new Date(2026, 9, 17, 0, 1))).toBe("2026-10-17"));
  it("preserves review paragraphs and permits clearing", () => {
    const change = vi.fn();
    render(
      <ReadingFields
        value={{ startDate: "", completionDate: "", review: "first\n\nsecond" }}
        onChange={change}
      />,
    );
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("first\n\nsecond");
    fireEvent.change(input, { target: { value: "" } });
    expect(change).toHaveBeenCalledWith({
      startDate: "",
      completionDate: "",
      review: "",
    });
  });
  it("requires a title for manual registration", () => {
    render(<MetadataFields value={emptyMetadata()} onChange={() => {}} />);
    expect(screen.getByLabelText("Title")).toBeRequired();
  });
  it("shows a fallback when a provider cover fails", () => {
    render(
      <BookCover
        url="https://covers.openlibrary.org/b/id/1-M.jpg"
        title="My book"
      />,
    );
    fireEvent.error(screen.getByRole("img"));
    expect(screen.getByText("My book")).toBeVisible();
  });
});
