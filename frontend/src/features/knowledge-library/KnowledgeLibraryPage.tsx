import { useEffect, useRef, useState } from "react";
import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import {
  BookOpen,
  Plus,
  LogOut,
  Library,
  LayoutGrid,
  List,
  ArrowUpRight,
  Leaf,
  Sun,
  MousePointer2,
} from "lucide-react";
import { request, errorMessage } from "../../shared/api";
import { AddBookDialog } from "./AddBookDialog";
import { BookDetailsDialog } from "./BookDetailsDialog";
import { BookCover } from "./BookCover";
import { packShelves, spineInk } from "./utils";
import type { LibraryBook, LibraryPage } from "./types";
export function KnowledgeLibraryPage({
  email,
  logout,
}: {
  email: string;
  logout: () => void;
}) {
  const client = useQueryClient(),
    caseRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(700),
    [add, setAdd] = useState(false),
    [selected, setSelected] = useState<LibraryBook | null>(null),
    [list, setList] = useState(false),
    [notice, setNotice] = useState(""),
    [newId, setNewId] = useState("");
  const query = useInfiniteQuery({
    queryKey: ["library"],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      request<LibraryPage>(`/library/books?page=${pageParam}&size=100`),
    getNextPageParam: (last, all) => (last.hasMore ? all.length : undefined),
  });
  const books = query.data?.pages.flatMap((p) => p.items) ?? [],
    total = query.data?.pages[0].total ?? 0,
    shelves = packShelves(books, Math.max(130, width - 68));
  useEffect(() => {
    if (!caseRef.current) return;
    const observer = new ResizeObserver((entries) =>
      setWidth(entries[0].contentRect.width),
    );
    observer.observe(caseRef.current);
    return () => observer.disconnect();
  }, [list, query.isPending]);
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(""), 5000);
    return () => clearTimeout(timer);
  }, [notice]);
  function refresh() {
    client.invalidateQueries({ queryKey: ["library"] });
  }
  return (
    <div className="app-layout">
      <aside className="sidebar">
        <a className="brand" href="/library">
          <BookOpen size={25} />
          <span>
            life tracker<span className="brand-dot">.</span>
          </span>
        </a>
        <p className="nav-caption">YOUR SPACES</p>
        <a className="nav-item nav-active" href="/library">
          <Library size={19} />
          <span>Knowledge Library</span>
          <span className="nav-indicator" />
        </a>
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <Leaf size={22} />
            <p>
              A little progress,
              <br />a little every day.
            </p>
          </div>
          <div className="account">
            <span className="avatar">{email[0].toUpperCase()}</span>
            <div>
              <strong>Your personal space</strong>
              <span title={email}>{email}</span>
            </div>
            <button
              aria-label="Sign out"
              className="icon-button"
              onClick={logout}
            >
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <span>
            Life Tracker <span className="breadcrumb-separator">/</span>{" "}
            <strong>Knowledge Library</strong>
          </span>
          <span className="private-label">
            <span />
            PRIVATE LIBRARY
          </span>
        </header>
        <div className="page-content">
          <div className="page-title-row">
            <div>
              <p className="eyebrow">
                <span className="little-line" /> A HOME FOR WHAT YOU’VE READ
              </p>
              <h1>
                Your knowledge library<span>.</span>
              </h1>
              <p className="page-subtitle">
                Stories you’ve lived. Ideas you’ve kept. A shelf that grows with
                you.
              </p>
            </div>
            <button className="primary add-main" onClick={() => setAdd(true)}>
              <Plus size={18} />
              Add a book
            </button>
          </div>
          <div className="library-toolbar">
            <div>
              <BookOpen size={18} />
              <strong>{total}</strong>
              <span>{total === 1 ? "book completed" : "books completed"}</span>
              <span className="toolbar-dot">·</span>
              <span className="toolbar-aside">Every spine tells a story</span>
            </div>
            <div className="view-switch" aria-label="Library view">
              <button
                className={!list ? "active" : ""}
                aria-label="Bookshelf view"
                aria-pressed={!list}
                onClick={() => setList(false)}
              >
                <LayoutGrid size={17} />
              </button>
              <button
                className={list ? "active" : ""}
                aria-label="List view"
                aria-pressed={list}
                onClick={() => setList(true)}
              >
                <List size={18} />
              </button>
            </div>
          </div>
          {query.isPending ? (
            <div className="loading-room" role="status">
              Dusting off the shelves…
            </div>
          ) : query.isError ? (
            <div className="error" role="alert">
              {errorMessage(query.error)}{" "}
              <button onClick={() => query.refetch()}>Try again</button>
            </div>
          ) : list ? (
            <div className="library-list">
              {!books.length ? (
                <div className="list-empty">
                  Your shelf is waiting for its first book.
                  <button className="text-link" onClick={() => setAdd(true)}>
                    Add your first book
                  </button>
                </div>
              ) : (
                books.map((book) => (
                  <button
                    className="library-list-book"
                    key={book.id}
                    onClick={() => setSelected(book)}
                  >
                    <BookCover
                      url={book.coverUrl}
                      title={book.metadata.title}
                    />
                    <div>
                      <strong>{book.metadata.title}</strong>
                      <span>{book.metadata.authors.join(", ")}</span>
                      <small>
                        {book.sessions.length}{" "}
                        {book.sessions.length === 1 ? "reading" : "readings"}
                      </small>
                    </div>
                    <ArrowUpRight size={19} />
                  </button>
                ))
              )}
            </div>
          ) : (
            <section className="library-room" aria-label="Your virtual library">
              <div className="room-light" />
              <div className="room-caption">
                <Sun size={14} />
                <span>A GOOD DAY TO TURN A PAGE</span>
              </div>
              <div className="bookcase" ref={caseRef}>
                <div className="bookcase-crown" />
                {shelves.map((shelf, i) => (
                  <div className="shelf" key={i}>
                    <div className="shelf-back" />
                    <div className="shelf-books">
                      {shelf.map((book) => (
                        <button
                          key={book.id}
                          aria-label={`Open ${book.metadata.title} by ${book.metadata.authors.join(", ") || "unknown author"}`}
                          title={book.metadata.title}
                          className={`book-spine ${book.id === newId ? "new-book" : ""}`}
                          style={{
                            width: book.spineWidth,
                            height: book.spineHeight,
                            color: spineInk(book.spineColor),
                            backgroundColor: book.spineColor,
                          }}
                          onClick={() => setSelected(book)}
                        >
                          <span className="spine-rule" />
                          <span className="spine-title">
                            {book.metadata.title}
                          </span>
                          <span className="spine-author">
                            {book.metadata.authors[0] ?? "PERSONAL LIBRARY"}
                          </span>
                          <span className="spine-ornament" aria-hidden="true">✦</span>
                        </button>
                      ))}
                    </div>
                    {!books.length && i === 1 && (
                      <div className="empty-shelf-message">
                        <BookOpen size={30} strokeWidth={1} />
                        <h2>A little empty. Full of possibility.</h2>
                        <p>Your next finished book belongs right here.</p>
                        <button onClick={() => setAdd(true)}>
                          Begin your collection <ArrowUpRight size={14} />
                        </button>
                      </div>
                    )}
                    <div className="shelf-edge" />
                  </div>
                ))}
                <div className="bookcase-base" />
              </div>
              <div className="desk-area">
                <div className="plant" aria-hidden="true">
                  <div className="plant-leaves">
                    <i />
                    <i />
                    <i />
                    <i />
                    <i />
                  </div>
                  <div className="plant-pot" />
                </div>
                <button
                  className="computer-workstation"
                  aria-label="Open Add Book using the computer"
                  onClick={() => setAdd(true)}
                >
                  <span className="computer-monitor">
                    <span className="monitor-screen">
                      <BookOpen size={25} />
                      <strong>Your next chapter</strong>
                      <span>
                        ADD A FINISHED BOOK <Plus size={10} />
                      </span>
                    </span>
                    <i />
                  </span>
                  <span className="monitor-neck" />
                  <span className="monitor-foot" />
                  <span className="keyboard" />
                  <span className="computer-mouse" />
                </button>
                <div className="desk-books" aria-hidden="true">
                  <i />
                  <i />
                  <i />
                  <div className="coffee-cup" />
                </div>
                <div className="desk-surface" />
                <div className="desk-leg left" />
                <div className="desk-leg right" />
              </div>
              <p className="room-hint">
                <MousePointer2 size={14} />A new book for the shelf? Start at
                your desk.
              </p>
            </section>
          )}
          {query.hasNextPage && (
            <div className="load-more">
              <button
                className="secondary"
                disabled={query.isFetchingNextPage}
                onClick={() => query.fetchNextPage()}
              >
                {query.isFetchingNextPage
                  ? "Loading…"
                  : `Load more books (${books.length} of ${total})`}
              </button>
            </div>
          )}
          <footer className="library-footer">
            <span>
              <Leaf size={14} /> Growth looks good on your shelf.
            </span>
            <span>MADE OF SMALL, MEANINGFUL CHAPTERS</span>
          </footer>
        </div>
      </main>
      {add && (
        <AddBookDialog
          onClose={() => setAdd(false)}
          onSaved={(book) => {
            setAdd(false);
            setNewId(book.id);
            setNotice("A new chapter, now on your shelf.");
            refresh();
          }}
        />
      )}
      {selected && (
        <BookDetailsDialog
          key={selected.id}
          book={selected}
          onClose={() => setSelected(null)}
          onUpdated={(book) => {
            setSelected(book);
            refresh();
          }}
          onDeleted={() => {
            setSelected(null);
            setNotice("Book removed from your library.");
            refresh();
          }}
        />
      )}
      {notice && (
        <div className="toast" role="status">
          <BookOpen size={18} />
          {notice}
        </div>
      )}
    </div>
  );
}
