import { lazy, Suspense, useState } from "react";
import { Upload, Paintbrush } from "lucide-react";
import { request, errorMessage } from "../../shared/api";
import type { Artwork } from "./types";
import { BookCover } from "./BookCover";
const DrawingCanvas = lazy(() => import("./DrawingCanvas"));
export function CoverCustomizer({
  title,
  providerCover,
  artwork,
  onChange,
  onRemove,
  onBusyChange,
}: {
  title: string;
  providerCover: string | null;
  artwork: Artwork[];
  onChange: (a: Artwork) => Promise<void> | void;
  onRemove: (type: Artwork["type"]) => Promise<void> | void;
  onBusyChange?: (busy: boolean) => void;
}) {
  const [drawing, setDrawing] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  function changeBusy(next: boolean) {
    setBusy(next);
    onBusyChange?.(next);
  }
  const effective =
    artwork.find((a) => a.type === "DRAWING") ??
    artwork.find((a) => a.type === "UPLOAD");
  async function upload(file: File, type: Artwork["type"]) {
    setError("");
    if (!["image/png", "image/jpeg"].includes(file.type)) {
      setError("Choose a PNG or JPEG image.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Choose an image no larger than 5 MiB.");
      return;
    }
    changeBusy(true);
    try {
      const data = new FormData();
      data.append("file", file);
      const a = await request<Artwork>("/artwork?type=" + type, {
        method: "POST",
        body: data,
      });
      await onChange(a);
      setDrawing(false);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      changeBusy(false);
    }
  }
  return (
    <section className="cover-customizer">
      <div className="cover-customizer-top">
        <BookCover url={effective?.url ?? providerCover} title={title} />
        <div>
          <h3>A cover that feels like yours</h3>
          <p className="muted">
            Keep the original, upload a favorite, or draw something personal.
          </p>
          <label className="secondary upload-button">
            <Upload size={16} />
            Upload image
            <input
              type="file"
              accept="image/png,image/jpeg"
              disabled={busy}
              onChange={(e) => {
                if (e.target.files?.[0]) upload(e.target.files[0], "UPLOAD");
                e.target.value = "";
              }}
            />
          </label>
          <button
            type="button"
            className="secondary"
            disabled={busy}
            onClick={() => setDrawing((v) => !v)}
          >
            <Paintbrush size={16} />
            {drawing ? "Close canvas" : "Draw a cover"}
          </button>
          <small>PNG or JPEG · 5 MiB max · 16 megapixels max</small>
        </div>
      </div>
      {artwork.map((a) => (
        <div className="artwork-slot" key={a.type}>
          <span>{a.type === "DRAWING" ? "Your drawing" : "Your upload"}</span>
          <button
            type="button"
            className="text-link"
            disabled={busy}
            onClick={async () => {
              changeBusy(true);
              setError("");
              try {
                await onRemove(a.type);
              } catch (e) {
                setError(errorMessage(e));
              } finally {
                changeBusy(false);
              }
            }}
          >
            Remove {a.type.toLowerCase()}
          </button>
        </div>
      ))}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {busy && <p role="status">Saving artwork…</p>}
      {drawing && (
        <Suspense fallback={<p>Opening canvas…</p>}>
          <DrawingCanvas
            onSave={(file) => upload(file, "DRAWING")}
            busy={busy}
          />
        </Suspense>
      )}
    </section>
  );
}
