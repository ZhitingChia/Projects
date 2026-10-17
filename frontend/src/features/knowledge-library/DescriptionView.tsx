import { useEffect, useState } from "react";
import { request, errorMessage } from "../../shared/api";
import type { Description } from "./types";
/** Deliberately transient: never put external descriptions in a query cache or storage. */
export function DescriptionView({ path }: { path: string | null }) {
  const [data, setData] = useState<Description | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [attempt, setAttempt] = useState(0);
  useEffect(() => {
    setData(null);
    setError("");
    if (!path) return;
    const controller = new AbortController();
    setBusy(true);
    request<Description>(path, { signal: controller.signal })
      .then(setData)
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setBusy(false);
      });
    return () => controller.abort();
  }, [path, attempt]);
  return (
    <section className="description">
      <h3>About this book</h3>
      {busy ? (
        <p className="muted" role="status">
          Fetching description…
        </p>
      ) : error ? (
        <p className="muted">
          {error}{" "}
          <button
            type="button"
            className="text-link"
            onClick={() => setAttempt((x) => x + 1)}
          >
            Retry
          </button>
        </p>
      ) : (
        <>
          <p className="preserve-lines">
            {data?.text ||
              "No external description is available. Your own reflections are always welcome."}
          </p>
          {data?.text && (
            <small>
              Source: {data.source}
              {data.workLevel ? " · Work-level description" : ""}
            </small>
          )}
        </>
      )}
    </section>
  );
}
