import type { LibraryBook } from "./types";
export function spineInk(color: string) {
  const channels = [1, 3, 5]
    .map((i) => parseInt(color.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  const luminance =
    channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
  return luminance > 0.179 ? "#000000" : "#ffffff";
}
export function localDate(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
export function packShelves(
  books: LibraryBook[],
  width: number,
): LibraryBook[][] {
  const shelves: LibraryBook[][] = [[]];
  let used = 0;
  for (const book of books) {
    const footprint = Math.min(book.spineWidth, width) + 5;
    if (used && used + footprint > width) {
      shelves.push([]);
      used = 0;
    }
    shelves[shelves.length - 1].push(book);
    used += footprint;
  }
  while (shelves.length < 3) shelves.push([]);
  return shelves;
}
