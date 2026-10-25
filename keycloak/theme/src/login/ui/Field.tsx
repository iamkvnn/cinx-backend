import { useState, type ReactNode } from "react";
import { CircleAlert, Eye, EyeOff } from "lucide-react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import { REVEAL_EVENT } from "../owl/CinxOwl";

/** Label, control and the field's own error (Keycloak sends errors as HTML, sanitised here). */
export function Field(props: {
  id: string;
  label: ReactNode;
  optional?: ReactNode;
  error?: string;
  /** a link after the error, such as "Đăng nhập" when the email already has an account */
  errorAction?: ReactNode;
  children: ReactNode;
  aside?: ReactNode;
}) {
  const { id, label, optional, error, errorAction, children, aside } = props;
  return (
    <div className="field">
      <div className="field-head">
        <label htmlFor={id}>
          {label}
          {optional ? <span className="opt">{optional}</span> : null}
        </label>
        {aside}
      </div>
      {children}
      {error ? (
        <p className="err" id={`${id}-error`} aria-live="polite">
          <CircleAlert aria-hidden />
          <span dangerouslySetInnerHTML={{ __html: kcSanitize(error) }} />
          {errorAction}
        </p>
      ) : null}
    </div>
  );
}

/** A password input with the eye toggle; showing it makes the owl peek. */
export function PasswordInput(props: {
  id: string;
  name: string;
  autoComplete: "current-password" | "new-password";
  invalid?: boolean;
  autoFocus?: boolean;
  showLabel: string;
  hideLabel: string;
  placeholder?: string;
}) {
  const { id, name, autoComplete, invalid, autoFocus, showLabel, hideLabel, placeholder } = props;
  const [shown, setShown] = useState(false);
  return (
    <div className="input-wrap" data-invalid={invalid || undefined}>
      <input
        id={id}
        name={name}
        type={shown ? "text" : "password"}
        autoComplete={autoComplete}
        autoFocus={autoFocus}
        placeholder={placeholder}
        aria-invalid={invalid || undefined}
        aria-describedby={invalid ? `${id}-error` : undefined}
      />
      <button
        type="button"
        className={shown ? "eye is-on" : "eye"}
        aria-label={shown ? hideLabel : showLabel}
        aria-controls={id}
        onClick={() => {
          setShown(!shown);
          document.dispatchEvent(new CustomEvent(REVEAL_EVENT, { detail: !shown }));
          document.getElementById(id)?.focus();
        }}
      >
        {shown ? <EyeOff aria-hidden /> : <Eye aria-hidden />}
      </button>
    </div>
  );
}

export function Checkbox(props: { id: string; name: string; label: ReactNode; defaultChecked?: boolean; value?: string }) {
  const { id, name, label, defaultChecked, value } = props;
  return (
    <label className="check" htmlFor={id}>
      <input type="checkbox" id={id} name={name} defaultChecked={defaultChecked} value={value} />
      <span>{label}</span>
    </label>
  );
}
