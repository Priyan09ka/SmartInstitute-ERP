/** Prefer server JSON `message` (our AllError), then Spring Boot error fields, then raw body snippet. */
function messageFromErrorResponseText(text, httpStatus) {
  if (!text || !String(text).trim()) {
    return httpStatus != null ? `Empty response body (HTTP ${httpStatus})` : "Request failed";
  }
  const raw = String(text).trim();
  if (raw.startsWith("<")) {
    return "Server returned HTML instead of JSON — check the URL and backend logs.";
  }
  try {
    const body = JSON.parse(text);
    if (typeof body.message === "string" && body.message.trim()) return body.message.trim();
    if (typeof body.detail === "string" && body.detail.trim()) return body.detail.trim();
    if (Array.isArray(body.errors) && body.errors.length) {
      const first = body.errors[0];
      if (typeof first === "string") return first;
      if (first && typeof first.defaultMessage === "string") return first.defaultMessage;
    }
    if (typeof body.error === "string" && body.error && body.error !== "Bad Request") return body.error;
    if (body.status != null && body.path) {
      const parts = [body.message, body.detail, body.error].filter((x) => typeof x === "string" && x.trim());
      if (parts.length) return parts.join(" — ");
      return `HTTP ${body.status} ${body.path}`;
    }
  } catch (_) {
    /* not JSON */
  }
  const s = raw;
  return s.length > 400 ? `${s.slice(0, 400)}…` : s;
}

export const apiRequest = async (url, method = "GET", body) => {
  const token = localStorage.getItem("token");

  const headers = {
    "Content-Type": "application/json",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`http://localhost:8081${url}`, {
    method,
    headers,
    credentials: "include",
    body: body ? JSON.stringify(body) : null,
  });

  const text = await res.text();

  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      window.location.href = "/";
      return;
    }
    if (res.status === 403 && !token) {
      window.location.href = "/";
      return;
    }
    throw new Error(`[${res.status}] ${messageFromErrorResponseText(text, res.status)}`);
  }

  if (!text || text.trim() === "") return null;
  try {
    return JSON.parse(text);
  } catch (_) {
    return null;
  }
};

/** Multipart upload (do not set Content-Type; browser sets boundary). */
export const apiUploadMultipart = async (url, formData) => {
  const token = localStorage.getItem("token");
  const headers = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`http://localhost:8081${url}`, {
    method: "POST",
    headers,
    credentials: "include",
    body: formData,
  });

  const text = await res.text();

  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      window.location.href = "/";
      return;
    }
    const detail = messageFromErrorResponseText(text, res.status);
    const msg = `[${res.status}] ${detail}`;
    console.error("[apiUploadMultipart]", url, msg, text?.slice?.(0, 500));
    throw new Error(msg);
  }

  if (!text || text.trim() === "") return null;
  try {
    return JSON.parse(text);
  } catch (_) {
    return null;
  }
};