import { useState } from "react";
import { Pencil, Plus, Trash2, ArrowLeft } from "lucide-react";
import { Modal } from "../../shared/Dialog";
import { send, errorMessage } from "../../shared/api";
import { BookCover } from "./BookCover";
import { DescriptionView } from "./DescriptionView";
import { ReadingFields, type ReadingDraft } from "./ReadingFields";
import { MetadataFields } from "./MetadataFields";
import { CoverCustomizer } from "./CoverCustomizer";
import { localDate } from "./utils";
import { uuid } from "../../shared/uuid";
import type { LibraryBook, Metadata, Session } from "./types";
export function BookDetailsDialog({
  book,
  onClose,
  onUpdated,
  onDeleted,
}: {
  book: LibraryBook;
  onClose: () => void;
  onUpdated: (b: LibraryBook) => void;
  onDeleted: () => void;
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [mode, setMode] = useState<"view" | "metadata" | "cover" | "session">(
      "view",
    ),
    [session, setSession] = useState<Session | null>(null);
  const [reading, setReading] = useState<ReadingDraft>({
      startDate: "",
      completionDate: "",
      review: "",
    }),
    [metadata, setMetadata] = useState<Metadata>(book.metadata),
    [sessionRequestId, setSessionRequestId] = useState(() => uuid());
  async function act(fn: () => Promise<void>) {
    setBusy(true);
    setError("");
    try {
      await fn();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  function editSession(s: Session | null) {
    setError("");
    setSession(s);
    setSessionRequestId(uuid());
    setReading({
      startDate: s?.startDate ?? "",
      completionDate: s?.completionDate ?? "",
      review: s?.review ?? "",
    });
    setMode("session");
  }
  const m = book.metadata;
  return (
    <Modal
      title={m.title}
      description={m.authors.join(", ") || "A book in your personal library"}
      onClose={() => !busy && onClose()}
      wide
    >
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {mode === "view" ? (
        <>
          <div className="book-detail-top">
            <BookCover
              large
              url={book.coverUrl}
              title={m.title}
              author={m.authors.join(", ")}
            />
            <div>
              <span className="status-pill">Completed</span>
              {m.subtitle && <h3>{m.subtitle}</h3>}
              <dl className="metadata-list">
                {[
                  ["ISBN-10", m.isbn10],
                  ["ISBN-13", m.isbn13],
                  ["Publisher", m.publisher],
                  ["Published", m.publicationDate],
                  ["Language", m.language],
                  ["Pages", m.pageCount],
                ]
                  .filter(([, v]) => v)
                  .map(([k, v]) => (
                    <div key={k}>
                      <dt>{k}</dt>
                      <dd>{v}</dd>
                    </div>
                  ))}
              </dl>
              <div className="button-row">
                <button
                  className="secondary"
                  onClick={() => {
                    setMetadata(book.metadata);
                    setMode("metadata");
                  }}
                >
                  <Pencil size={15} />
                  Edit metadata
                </button>
                <button className="secondary" onClick={() => setMode("cover")}>
                  Customize cover
                </button>
              </div>
            </div>
          </div>
          <DescriptionView path={`/library/books/${book.id}/description`} />
          <section className="reading-history">
            <div className="section-heading">
              <h3>Your reading story</h3>
              <button className="secondary" onClick={() => editSession(null)}>
                <Plus size={15} />
                Add reread
              </button>
            </div>
            {book.sessions.map((s, i) => (
              <article className="reading-session" key={s.id}>
                <div className="section-heading">
                  <div>
                    <span className="eyebrow">
                      {book.sessions.length > 1
                        ? `READING ${book.sessions.length - i}`
                        : "A CHAPTER COMPLETED"}
                    </span>
                    <p className="session-dates">
                      {s.startDate ? `${s.startDate} → ` : ""}
                      {s.completionDate}
                    </p>
                  </div>
                  <div className="button-row">
                    <button
                      className="icon-button"
                      aria-label={`Edit reading completed ${s.completionDate}`}
                      onClick={() => editSession(s)}
                    >
                      <Pencil size={16} />
                    </button>
                    <button
                      className="icon-button"
                      aria-label={`Delete reading completed ${s.completionDate}`}
                      disabled={busy}
                      onClick={() => {
                        if (book.sessions.length === 1) {
                          setError(
                            "This is the final reading session. Delete the book to remove it.",
                          );
                          return;
                        }
                        if (
                          window.confirm(
                            "Delete this reading session and its review?",
                          )
                        )
                          act(async () => {
                            await send(
                              `/library/books/${book.id}/sessions/${s.id}`,
                              "DELETE",
                            );
                            onUpdated({
                              ...book,
                              sessions: book.sessions.filter(
                                (x) => x.id !== s.id,
                              ),
                            });
                          });
                      }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
                <p className={s.review ? "preserve-lines" : "muted"}>
                  {s.review ||
                    "No reflections yet. There’s always room for a thought."}
                </p>
              </article>
            ))}
          </section>
          <div className="danger-zone">
            <small>Added {new Date(book.dateAdded).toLocaleDateString()}</small>
            <button
              className="danger-button"
              disabled={busy}
              onClick={() => {
                if (
                  window.confirm(
                    "Remove this book, all reading sessions, reviews, and custom covers from your library?",
                  )
                )
                  act(async () => {
                    await send(`/library/books/${book.id}`, "DELETE");
                    onDeleted();
                  });
              }}
            >
              <Trash2 size={15} />
              Delete from library
            </button>
          </div>
        </>
      ) : (
        <>
          <button
            className="text-link back-link"
            disabled={busy}
            onClick={() => {
              setMode("view");
              setError("");
            }}
          >
            <ArrowLeft size={15} />
            Back to book
          </button>
          {mode === "metadata" && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                act(async () => {
                  const b = await send<LibraryBook>(
                    `/library/books/${book.id}`,
                    "PATCH",
                    { version: book.version, metadata },
                  );
                  onUpdated(b);
                  setMode("view");
                });
              }}
            >
              <MetadataFields
                value={metadata}
                onChange={setMetadata}
                identityLocked
              />
              <div className="dialog-actions">
                <span className="muted">
                  Corrections are private to your library.
                </span>
                <button className="primary" disabled={busy}>
                  Save metadata
                </button>
              </div>
            </form>
          )}
          {mode === "session" && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                act(async () => {
                  const path = `/library/books/${book.id}/sessions${session ? "/" + session.id : ""}`;
                  const b = await send<LibraryBook>(
                    path,
                    session ? "PATCH" : "POST",
                    session
                      ? {
                          version: session.version,
                          startDate: reading.startDate || null,
                          completionDate: reading.completionDate,
                          review: reading.review || null,
                        }
                      : {
                          requestId: sessionRequestId,
                          startDate: reading.startDate || null,
                          completionDate: reading.completionDate || null,
                          localDate: localDate(),
                          review: reading.review || null,
                        },
                  );
                  onUpdated(b);
                  setMode("view");
                });
              }}
            >
              <h3>
                {session
                  ? "Edit your reading"
                  : "Another visit to a familiar book"}
              </h3>
              <ReadingFields
                value={reading}
                onChange={setReading}
                completionRequired={!!session}
              />
              <div className="dialog-actions">
                <small>Clear the reflections field to remove its review.</small>
                <button className="primary" disabled={busy}>
                  {busy ? "Saving…" : "Save reading"}
                </button>
              </div>
            </form>
          )}
          {mode === "cover" && (
            <CoverCustomizer
              onBusyChange={setBusy}
              title={m.title}
              providerCover={m.coverUrl}
              artwork={book.artwork}
              onChange={async (a) => {
                const b = await send<LibraryBook>(
                  `/library/books/${book.id}/artwork/${a.type}`,
                  "PUT",
                  { artworkId: a.id },
                );
                onUpdated(b);
              }}
              onRemove={async (type) => {
                const b = await send<LibraryBook>(
                  `/library/books/${book.id}/artwork/${type}`,
                  "DELETE",
                );
                onUpdated(b);
              }}
            />
          )}
        </>
      )}
    </Modal>
  );
}
