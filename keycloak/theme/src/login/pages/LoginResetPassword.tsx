import { ArrowLeft } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";
import { Field } from "../ui/Field";

/** login-reset-password.ftl: one email field; Keycloak returns to the login page with the "check your inbox" note. */
export default function LoginResetPassword(props: PageProps<Extract<KcContext, { pageId: "login-reset-password.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, realm, auth, messagesPerField } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const error = messagesPerField.existsError("username") ? messagesPerField.get("username") : undefined;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={!error}
      headerNode={msg("emailForgotTitle")}
      subtitle={realm.duplicateEmailsAllowed ? msg("emailInstructionUsername") : msg("emailInstruction")}
    >
      <form id="kc-reset-password-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        <Field
          id="username"
          label={!realm.loginWithEmailAllowed ? msg("username") : !realm.registrationEmailAsUsername ? msg("usernameOrEmail") : msg("email")}
          error={error}
        >
          <input
            id="username"
            name="username"
            type="text"
            inputMode="email"
            autoFocus
            autoComplete="username"
            defaultValue={auth.attemptedUsername ?? ""}
            placeholder={msgStr("cinxEmailPlaceholder")}
            aria-invalid={!!error || undefined}
            aria-describedby={error ? "username-error" : undefined}
          />
        </Field>
        <Submit label={msgStr("cinxSendLink")} busyLabel={msg("cinxSending")} busy={busy} />
      </form>
      <a className="link back" href={url.loginUrl}>
        <ArrowLeft aria-hidden />
        {msg("backToLogin")}
      </a>
    </Template>
  );
}
