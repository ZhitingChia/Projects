import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";
import { request, send, errorMessage } from "../shared/api";
import { AuthPage } from "../features/auth/AuthPage";
import { KnowledgeLibraryPage } from "../features/knowledge-library/KnowledgeLibraryPage";
import { useState } from "react";
export function App() {
  const client = useQueryClient(),
    navigate = useNavigate();
  const [error, setError] = useState("");
  const me = useQuery({
    queryKey: ["me"],
    queryFn: () => request<{ email: string } | null>("/auth/me"),
    retry: false,
  });
  async function logout() {
    try {
      await send("/auth/logout", "POST");
      await client.cancelQueries();
      client.setQueryData(["me"], null);
      client.removeQueries({ queryKey: ["library"] });
      navigate("/login");
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  const loading = (
    <div className="loading-room" role="status">
      Opening your library…
    </div>
  );
  return (
    <>
      <Routes>
        {[
          "login",
          "register",
          "forgot-password",
          "verify-email",
          "reset-password",
        ].map((page) => (
          <Route
            key={page}
            path={"/" + page}
            element={
              <AuthPage
                key={page}
                page={page}
                onLogin={async () => {
                  await me.refetch();
                  navigate("/library");
                }}
              />
            }
          />
        ))}
        <Route
          path="/library"
          element={
            me.isPending ? (
              loading
            ) : me.data ? (
              <KnowledgeLibraryPage email={me.data.email} logout={logout} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route
          path="*"
          element={
            me.isPending ? (
              loading
            ) : (
              <Navigate to={me.data ? "/library" : "/login"} replace />
            )
          }
        />
      </Routes>
      {error && (
        <div className="toast" role="alert">
          {error}
          <button onClick={() => setError("")}>Dismiss</button>
        </div>
      )}
    </>
  );
}
