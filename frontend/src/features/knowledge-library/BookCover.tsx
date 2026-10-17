import { useEffect, useState } from "react";
import { BookOpen } from "lucide-react";
export function BookCover({
  url,
  title,
  author = "",
  large = false,
}: {
  url: string | null;
  title: string;
  author?: string;
  large?: boolean;
}) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [url]);
  return (
    <div className={`book-cover ${large ? "cover-large" : ""}`}>
      {url && !failed ? (
        <img
          src={url}
          onError={() => setFailed(true)}
          alt={`Cover of ${title}`}
          loading="lazy"
          referrerPolicy="no-referrer"
        />
      ) : (
        <div className="cover-template">
          <span>THE PERSONAL LIBRARY</span>
          <BookOpen size={28} />
          <strong>{title || "Your next chapter"}</strong>
          <small>{author}</small>
          <i />
        </div>
      )}
    </div>
  );
}
