import { useState, type ReactNode } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

/** Marks a form as posting, so its submit button shows the spinner and cannot be pressed twice. */
export function useSubmitting() {
  const [busy, setBusy] = useState(false);
  return { busy, onSubmit: () => { setBusy(true); return true; } };
}

/** The primary action (tactile, with a spinner while the form posts). `name`/`value` are sent with the form. */
export function Submit(props: { label: ReactNode; busyLabel?: ReactNode; busy?: boolean; name?: string; value?: string; id?: string }) {
  const { label, busyLabel, busy = false, name, value, id } = props;
  return (
    <button type="submit" className="primary" id={id} name={name} value={value} disabled={busy} aria-busy={busy || undefined}>
      {busy ? <span className="spin" aria-hidden /> : null}
      {busy && busyLabel ? busyLabel : label}
    </button>
  );
}

const GOOGLE = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z" />
    <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
    <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z" />
    <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z" />
  </svg>
);

type Provider = { alias: string; displayName: string; loginUrl: string };

/** One button per identity provider (Google today), labelled "<verb> <provider>". */
export function ProviderButtons(props: { providers: Provider[] | undefined; label: (name: string) => string }) {
  const { providers, label } = props;
  if (!providers?.length) return null;
  return (
    <div className="providers">
      {providers.map((p) => (
        <a key={p.alias} id={`social-${p.alias}`} className="ghost" href={p.loginUrl}>
          {p.alias === "google" ? GOOGLE : null}
          <span>{label(kcSanitize(p.displayName).replace(/<[^>]*>/g, ""))}</span>
        </a>
      ))}
    </div>
  );
}
