import { createRoot } from "react-dom/client";
import { StrictMode } from "react";
import { KcPage } from "./kc.gen";

// `pnpm dev` has no Keycloak: render a mock page, chosen with ?page=<id>.ftl (dev builds only)
if (import.meta.env.DEV && !window.kcContext) {
  const { getKcContextMock } = await import("./login/mock");
  const pageId = (new URLSearchParams(location.search).get("page") ?? "login.ftl") as Parameters<typeof getKcContextMock>[0]["pageId"];
  window.kcContext = getKcContextMock({ pageId, overrides: {} });
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>{window.kcContext ? <KcPage kcContext={window.kcContext} /> : <h1>No Keycloak context</h1>}</StrictMode>,
);
