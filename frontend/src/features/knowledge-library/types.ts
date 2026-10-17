export type Metadata = {
  title: string;
  subtitle: string | null;
  authors: string[];
  isbn10: string | null;
  isbn13: string | null;
  publisher: string | null;
  publicationDate: string | null;
  language: string | null;
  pageCount: number | null;
  coverUrl: string | null;
  provider: string | null;
  editionId: string | null;
  workId: string | null;
};
export type Session = {
  id: string;
  version: number;
  startDate: string | null;
  completionDate: string;
  review: string | null;
};
export type Artwork = { id: string; type: "DRAWING" | "UPLOAD"; url: string };
export type LibraryBook = {
  id: string;
  version: number;
  metadata: Metadata;
  readingStatus: string;
  dateAdded: string;
  spineColor: string;
  spineWidth: number;
  spineHeight: number;
  coverUrl: string | null;
  artwork: Artwork[];
  sessions: Session[];
};
export type LibraryPage = {
  items: LibraryBook[];
  hasMore: boolean;
  total: number;
};
export type Description = {
  text: string | null;
  source: string;
  workLevel: boolean;
};
export const emptyMetadata = (): Metadata => ({
  title: "",
  subtitle: null,
  authors: [],
  isbn10: null,
  isbn13: null,
  publisher: null,
  publicationDate: null,
  language: null,
  pageCount: null,
  coverUrl: null,
  provider: null,
  editionId: null,
  workId: null,
});
