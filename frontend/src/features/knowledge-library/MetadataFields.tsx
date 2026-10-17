import type { Metadata } from "./types";
import { useState } from "react";
export function MetadataFields({
  value,
  onChange,
  identityLocked = false,
}: {
  value: Metadata;
  onChange: (m: Metadata) => void;
  identityLocked?: boolean;
}) {
  const [authorText, setAuthorText] = useState(value.authors.join("; "));
  function text(key: keyof Metadata, label: string, required = false) {
    return (
      <label key={key}>
        {label}
        <input
          required={required}
          maxLength={
            key === "language" ? 40 : key === "publicationDate" ? 100 : 500
          }
          value={String(value[key] ?? "")}
          onChange={(e) =>
            onChange({ ...value, [key]: e.target.value || null })
          }
        />
      </label>
    );
  }
  return (
    <div className="metadata-fields">
      <div className="form-grid">
        {text("title", "Title", true)}
        {text("subtitle", "Subtitle")}
        <label className="full-span">
          Authors <small>(separate with semicolons)</small>
          <input
            maxLength={3000}
            value={authorText}
            onChange={(e) => {
              setAuthorText(e.target.value);
              onChange({
                ...value,
                authors: e.target.value
                  .split(";")
                  .map((a) => a.trim())
                  .filter(Boolean),
              });
            }}
          />
        </label>
        <label>
          ISBN-10
          <input
            maxLength={20}
            readOnly={identityLocked}
            value={value.isbn10 ?? ""}
            onChange={(e) =>
              onChange({ ...value, isbn10: e.target.value || null })
            }
          />
        </label>
        <label>
          ISBN-13
          <input
            maxLength={25}
            readOnly={identityLocked}
            value={value.isbn13 ?? ""}
            onChange={(e) =>
              onChange({ ...value, isbn13: e.target.value || null })
            }
          />
        </label>
        {text("publisher", "Publisher")}
        {text("publicationDate", "Publication date or year")}
        {text("language", "Language")}
        <label>
          Page count
          <input
            type="number"
            min="1"
            max="100000"
            value={value.pageCount ?? ""}
            onChange={(e) =>
              onChange({
                ...value,
                pageCount: e.target.value ? Number(e.target.value) : null,
              })
            }
          />
        </label>
      </div>
      {identityLocked && (
        <small>
          To change the edition or ISBN, add the correct edition as a separate
          book.
        </small>
      )}
    </div>
  );
}
