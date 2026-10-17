export type ReadingDraft = {
  startDate: string;
  completionDate: string;
  review: string;
};
export function ReadingFields({
  value,
  onChange,
  completionRequired = false,
}: {
  value: ReadingDraft;
  onChange: (v: ReadingDraft) => void;
  completionRequired?: boolean;
}) {
  return (
    <div>
      <div className="form-grid">
        <label>
          Started <small>(optional)</small>
          <input
            type="date"
            value={value.startDate}
            onChange={(e) => onChange({ ...value, startDate: e.target.value })}
          />
        </label>
        <label>
          Finished{!completionRequired && <small> (defaults to today)</small>}
          <input
            type="date"
            required={completionRequired}
            min={value.startDate || undefined}
            value={value.completionDate}
            onChange={(e) =>
              onChange({ ...value, completionDate: e.target.value })
            }
          />
        </label>
      </div>
      <label>
        Your reflections <small>(optional)</small>
        <textarea
          rows={5}
          maxLength={20000}
          placeholder="What stayed with you? A thought, a feeling, a favorite lesson…"
          value={value.review}
          onChange={(e) => onChange({ ...value, review: e.target.value })}
        />
      </label>
    </div>
  );
}
