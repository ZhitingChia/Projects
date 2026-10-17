const root = "/api/v1";
let csrf: { token: string; headerName: string } | undefined;
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public fields?: Record<string, string>,
  ) {
    super(message);
  }
}
export async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = init.method ?? "GET";
  if (!["GET", "HEAD"].includes(method) && !csrf)
    csrf = await request("/auth/csrf");
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  if (csrf && !["GET", "HEAD"].includes(method))
    headers.set(csrf.headerName, csrf.token);
  const response = await fetch(root + path, {
    ...init,
    headers,
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok) {
    if (response.status === 403) csrf = undefined;
    const p = await response.json().catch(() => ({}));
    throw new ApiError(
      p.detail ?? "The request failed. Please try again.",
      response.status,
      p.errors,
    );
  }
  if (path === "/auth/login" || path === "/auth/logout") csrf = undefined;
  return response.status === 204 || response.status === 202
    ? (undefined as T)
    : response.json();
}
export const send = <T>(path: string, method: string, body?: unknown) =>
  request<T>(path, {
    method,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
export function errorMessage(error: unknown) {
  if (error instanceof ApiError && error.fields)
    return Object.entries(error.fields)
      .map(([field, message]) => `${field}: ${message}`)
      .join("; ");
  return error instanceof Error
    ? error.message
    : "Something went wrong. Please try again.";
}
