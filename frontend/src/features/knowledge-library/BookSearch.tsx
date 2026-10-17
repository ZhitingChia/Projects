import { useEffect, useRef, useState } from "react";
import { Search, ArrowRight, PenLine } from "lucide-react";
import { request, errorMessage } from "../../shared/api";
import { BookCover } from "./BookCover";
import type { Metadata } from "./types";
export function BookSearch({
  onSelect,
  onManual,
}: {
  onSelect: (m: Metadata) => void;
  onManual: () => void;
}) {
  const [title, setTitle] = useState(""),
    [author, setAuthor] = useState(""),
    [isbn, setIsbn] = useState(""),
    [page, setPage] = useState(0),
    [tick, setTick] = useState(0);
  const [results, setResults] = useState<Metadata[]>([]),
    [warnings, setWarnings] = useState<string[]>([]),
    [busy, setBusy] = useState(false),
    [selecting, setSelecting] = useState(false),
    [error, setError] = useState(""),
    [searched, setSearched] = useState(false);
  const selectController = useRef<AbortController | null>(null);
  useEffect(() => () => selectController.current?.abort(), []);
  useEffect(() => {
    if (!title.trim() && !author.trim() && !isbn.trim()) {
      setResults([]);
      setSearched(false);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setBusy(true);
      setError("");
      const query = new URLSearchParams({
        title,
        author,
        isbn,
        page: String(page),
      });
      request<{ items: Metadata[]; warnings: string[] }>(
        "/books/search?" + query,
        { signal: controller.signal },
      )
        .then((data) => {
          setResults(data.items);
          setWarnings(data.warnings);
          setSearched(true);
        })
        .catch((e) => {
          if (!controller.signal.aborted) setError(errorMessage(e));
        })
        .finally(() => {
          if (!controller.signal.aborted) setBusy(false);
        });
    }, 400);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [title, author, isbn, page, tick]);
  async function select(m: Metadata) {
    setSelecting(true);
    setError("");
    selectController.current?.abort();
    const controller = new AbortController();
    selectController.current = controller;
    try {
      const exact = await request<Metadata>(
        `/books/providers/${m.provider}/${m.editionId}`,
        { signal: controller.signal },
      );
      if (!controller.signal.aborted)
        onSelect({
          ...exact,
          authors: exact.authors.length ? exact.authors : m.authors,
        });
    } catch (e) {
      if (!controller.signal.aborted) setError(errorMessage(e));
    } finally {
      if (!controller.signal.aborted) setSelecting(false);
    }
  }
  return (
    <div>
      <form
        className="search-form"
        onSubmit={(e) => {
          e.preventDefault();
          setTick((t) => t + 1);
        }}
      >
        <label>
          Book title
          <div className="input-icon">
            <Search size={18} />
            <input
              autoFocus
              placeholder="A story, an idea, a title…"
              value={title}
              maxLength={300}
              onChange={(e) => {
                setTitle(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </label>
        <div className="form-grid">
          <label>
            Author
            <input
              value={author}
              maxLength={300}
              onChange={(e) => {
                setAuthor(e.target.value);
                setPage(0);
              }}
            />
          </label>
          <label>
            ISBN-10 or ISBN-13
            <input
              value={isbn}
              maxLength={40}
              onChange={(e) => {
                setIsbn(e.target.value);
                setPage(0);
              }}
            />
          </label>
        </div>
        <button
          className="secondary"
          disabled={
            busy ||
            selecting ||
            (!title.trim() && !author.trim() && !isbn.trim())
          }
        >
          Search books
        </button>
      </form>
      <div aria-live="polite">
        {busy && (
          <p role="status" className="muted">
            Looking through the stacks…
          </p>
        )}
        {selecting && <p role="status">Fetching this edition…</p>}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {warnings.map((w) => (
          <p key={w} className="notice">
            {w}
          </p>
        ))}
        {searched && !busy && !results.length && (
          <p className="muted">
            No matching editions found. Try another search or enter your book
            manually.
          </p>
        )}
      </div>
      <div className="search-results">
        {results.map((m) => (
          <button
            disabled={selecting || busy}
            key={m.provider + ":" + m.editionId}
            className="search-result"
            onClick={() => select(m)}
          >
            <BookCover url={m.coverUrl} title={m.title} />
            <div>
              <strong>{m.title}</strong>
              <span>{m.authors.join(", ") || "Author unavailable"}</span>
              <small>
                {[m.publicationDate, m.publisher, m.isbn13 ?? m.isbn10]
                  .filter(Boolean)
                  .join(" · ")}
              </small>
              <small>
                {m.provider === "googlebooks" ? "Google Books" : "Open Library"}{" "}
                · Select edition
              </small>
            </div>
            <ArrowRight size={18} />
          </button>
        ))}
      </div>
      {searched && (
        <div className="pagination">
          <button
            className="secondary"
            disabled={page === 0 || busy}
            onClick={() => setPage((p) => p - 1)}
          >
            Previous
          </button>
          <span>Page {page + 1}</span>
          <button
            className="secondary"
            disabled={!results.length || busy || page >= 20}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
      <div className="manual-entry">
        <PenLine size={20} />
        <div>
          <strong>Some books are harder to find.</strong>
          <p>Add any edition, even without an ISBN.</p>
        </div>
        <button className="secondary" onClick={onManual}>
          Enter manually
        </button>
      </div>
    </div>
  );
}
