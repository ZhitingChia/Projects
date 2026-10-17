import { useRef, useState, useEffect } from "react";
import { Undo2, Eraser, PenLine, Trash2 } from "lucide-react";
export default function DrawingCanvas({
  onSave,
  busy,
}: {
  onSave: (file: File) => void;
  busy: boolean;
}) {
  const ref = useRef<HTMLCanvasElement>(null),
    drawing = useRef(false),
    last = useRef<{ x: number; y: number } | null>(null),
    history = useRef<ImageData[]>([]);
  const [color, setColor] = useState("#315b4c"),
    [size, setSize] = useState(8),
    [eraser, setEraser] = useState(false),
    [count, setCount] = useState(0);
  useEffect(() => {
    const ctx = ref.current!.getContext("2d")!;
    ctx.fillStyle = "#f9f2e3";
    ctx.fillRect(0, 0, 400, 560);
  }, []);
  function snapshot() {
    const ctx = ref.current!.getContext("2d")!;
    history.current.push(ctx.getImageData(0, 0, 400, 560));
    if (history.current.length > 20) history.current.shift();
    setCount(history.current.length);
  }
  function undo() {
    const image = history.current.pop();
    if (image) ref.current!.getContext("2d")!.putImageData(image, 0, 0);
    setCount(history.current.length);
  }
  function point(e: React.PointerEvent<HTMLCanvasElement>) {
    const r = e.currentTarget.getBoundingClientRect();
    return {
      x: ((e.clientX - r.left) * 400) / r.width,
      y: ((e.clientY - r.top) * 560) / r.height,
    };
  }
  function stroke(p: { x: number; y: number }) {
    const ctx = ref.current!.getContext("2d")!;
    ctx.strokeStyle = eraser ? "#f9f2e3" : color;
    ctx.lineWidth = size;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    ctx.moveTo(last.current?.x ?? p.x, last.current?.y ?? p.y);
    ctx.lineTo(p.x + 0.01, p.y + 0.01);
    ctx.stroke();
    last.current = p;
  }
  return (
    <div className="drawing-editor">
      <p className="muted">
        Make this cover your own. Draw with a mouse, stylus, or touch.
      </p>
      <div className="drawing-tools">
        <button
          type="button"
          className={!eraser ? "active" : ""}
          aria-pressed={!eraser}
          onClick={() => setEraser(false)}
        >
          <PenLine size={16} />
          Pen
        </button>
        <button
          type="button"
          aria-pressed={eraser}
          className={eraser ? "active" : ""}
          onClick={() => setEraser(true)}
        >
          <Eraser size={16} />
          Eraser
        </button>
        <label>
          Color
          <input
            aria-label="Brush color"
            type="color"
            value={color}
            onChange={(e) => setColor(e.target.value)}
          />
        </label>
        <label>
          Brush size
          <input
            aria-label="Brush size"
            type="range"
            min="2"
            max="40"
            value={size}
            onChange={(e) => setSize(Number(e.target.value))}
          />
        </label>
        <button type="button" disabled={!count} onClick={undo}>
          <Undo2 size={16} />
          Undo
        </button>
        <button
          type="button"
          onClick={() => {
            snapshot();
            const ctx = ref.current!.getContext("2d")!;
            ctx.fillStyle = "#f9f2e3";
            ctx.fillRect(0, 0, 400, 560);
          }}
        >
          <Trash2 size={16} />
          Clear
        </button>
      </div>
      <canvas
        ref={ref}
        width={400}
        height={560}
        aria-label="Custom cover drawing canvas. Image upload is available as an alternative."
        onPointerDown={(e) => {
          snapshot();
          drawing.current = true;
          last.current = null;
          e.currentTarget.setPointerCapture(e.pointerId);
          stroke(point(e));
        }}
        onPointerMove={(e) => {
          if (drawing.current) stroke(point(e));
        }}
        onPointerUp={() => {
          drawing.current = false;
          last.current = null;
        }}
        onPointerCancel={() => {
          drawing.current = false;
          last.current = null;
        }}
      />
      <button
        type="button"
        className="primary"
        disabled={busy}
        onClick={() =>
          ref.current!.toBlob((blob) => {
            if (blob)
              onSave(new File([blob], "drawing.png", { type: "image/png" }));
          }, "image/png")
        }
      >
        {busy ? "Saving artwork…" : "Use this drawing"}
      </button>
    </div>
  );
}
