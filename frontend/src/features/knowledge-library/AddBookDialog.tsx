import { useState } from "react";
import { ArrowLeft, Check, ArrowRight } from "lucide-react";
import { Modal } from "../../shared/Dialog";
import { send, errorMessage } from "../../shared/api";
import { BookSearch } from "./BookSearch";
import { MetadataFields } from "./MetadataFields";
import { ReadingFields, type ReadingDraft } from "./ReadingFields";
import { CoverCustomizer } from "./CoverCustomizer";
import { DescriptionView } from "./DescriptionView";
import {
  emptyMetadata,
  type Metadata,
  type Artwork,
  type LibraryBook,
} from "./types";
import { localDate } from "./utils";
import { uuid } from "../../shared/uuid";
export function AddBookDialog({
  onClose,
  onSaved,
}: {
  onClose: () => void;
  onSaved: (b: LibraryBook) => void;
}) {
  const [step, setStep] = useState(0),
    [metadata, setMetadata] = useState<Metadata>(emptyMetadata),
    [reading, setReading] = useState<ReadingDraft>({
      startDate: "",
      completionDate: "",
      review: "",
    }),
    [artwork, setArtwork] = useState<Artwork[]>([]);
  const [busy, setBusy] = useState(false),
    [artworkBusy, setArtworkBusy] = useState(false),
    [error, setError] = useState(""),
    [confirm, setConfirm] = useState(false);
  const [requestId] = useState(uuid);
  const [sessionRequestId] = useState(uuid);
  async function save(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const book = await send<LibraryBook>("/library/books", "POST", {
        requestId,
        metadata,
        reading: {
          requestId: sessionRequestId,
          startDate: reading.startDate || null,
          completionDate: reading.completionDate || null,
          localDate: localDate(),
          review: reading.review || null,
        },
        drawingId: artwork.find((a) => a.type === "DRAWING")?.id ?? null,
        uploadId: artwork.find((a) => a.type === "UPLOAD")?.id ?? null,
        confirmPossibleDuplicate: confirm,
      });
      onSaved(book);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title="A new chapter for your shelf"
      description="Find your book, make it yours, and remember what it gave you."
      onClose={() => {
        if (
          !busy &&
          !artworkBusy &&
          window.confirm(
            "Close this form? Unsaved book information will be lost.",
          )
        )
          onClose();
      }}
      wide
    >
      <ol className="stepper">
        {["Find your book", "Confirm edition", "Your reading story"].map(
          (s, i) => (
            <li
              className={i === step ? "current" : i < step ? "done" : ""}
              key={s}
            >
              <span>{i < step ? <Check size={13} /> : i + 1}</span>
              {s}
            </li>
          ),
        )}
      </ol>
      {step === 0 ? (
        <BookSearch
          onSelect={(m) => {
            setMetadata(m);
            setStep(1);
          }}
          onManual={() => {
            setMetadata(emptyMetadata());
            setStep(1);
          }}
        />
      ) : step === 1 ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setStep(2);
          }}
        >
          <MetadataFields value={metadata} onChange={setMetadata} />
          {metadata.provider && (
            <button
              type="button"
              className="text-link"
              onClick={() =>
                setMetadata((m) => ({
                  ...m,
                  provider: null,
                  editionId: null,
                  workId: null,
                }))
              }
            >
              Use these details as a manual entry instead
            </button>
          )}
          <DescriptionView
            path={
              metadata.provider
                ? `/books/providers/${metadata.provider}/${metadata.editionId}/description`
                : null
            }
          />
          <div className="dialog-actions">
            <button
              className="secondary"
              type="button"
              onClick={() => setStep(0)}
            >
              <ArrowLeft size={16} />
              Search again
            </button>
            <button className="primary">
              Confirm this edition
              <ArrowRight size={16} />
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={save}>
          <div className="selected-book">
            <Check size={18} />
            <div>
              <strong>{metadata.title}</strong>
              <small>
                {metadata.authors.join(", ") || "Your personal edition"}
              </small>
            </div>
            <span className="status-pill">Completed</span>
          </div>
          <ReadingFields value={reading} onChange={setReading} />
          <CoverCustomizer
            onBusyChange={setArtworkBusy}
            title={metadata.title}
            providerCover={metadata.coverUrl}
            artwork={artwork}
            onChange={(a) =>
              setArtwork((old) => [...old.filter((x) => x.type !== a.type), a])
            }
            onRemove={(type) =>
              setArtwork((old) => old.filter((a) => a.type !== type))
            }
          />
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={confirm}
              onChange={(e) => setConfirm(e.target.checked)}
            />
            If a manual book has the same title as an existing book, I confirm
            this is a different edition.
          </label>
          {error && (
            <p role="alert" className="error">
              {error}
            </p>
          )}
          <div className="dialog-actions">
            <button
              className="secondary"
              type="button"
              disabled={busy || artworkBusy}
              onClick={() => setStep(1)}
            >
              <ArrowLeft size={16} />
              Back
            </button>
            <button className="primary" disabled={busy || artworkBusy}>
              {busy ? "Saving to your shelf…" : "Add to library"}
              <Check size={16} />
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
