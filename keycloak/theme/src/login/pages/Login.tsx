import { ArrowLeft } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template, { isAdminClient } from "../Template";
import { Alert } from "../ui/Alert";
import { ProviderButtons, Submit, useSubmitting } from "../ui/Buttons";
import { Checkbox, Field, PasswordInput } from "../ui/Field";

/** login.ftl: Google first, then email and password (Keycloakify's Login, restyled; WebAuthn conditional UI left out). */
export default function Login(props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { social, realm, url, usernameHidden, login, auth, registrationDisabled, messagesPerField } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const invalid = messagesPerField.existsError("username", "password");
  // operators sign in with password + OTP only (the step-up flow has no identity provider) and never sign up here
  const admin = isAdminClient(kcContext);
  // re-authenticating to link an identity provider: Keycloak hides the username and disables sign-up
  const linking = !!usernameHidden && registrationDisabled;
  const providers = admin || linking ? undefined : social?.providers;
  const whoShown = kcContext.auth?.showUsername && !kcContext.auth?.showResetCredentials;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={!invalid}
      headerNode={msg("loginAccountTitle")}
      subtitle={admin ? msg("cinxAdminLoginSubtitle") : msg("cinxLoginSubtitle")}
    >
      {invalid ? <Alert type="error" html={messagesPerField.getFirstError("username", "password")} /> : null}
      <ProviderButtons providers={providers} label={(name) => msgStr("cinxContinueWith", name)} />
      {realm.password && providers?.length ? <div className="or">{msg("cinxOrEmail")}</div> : null}
      {realm.password ? (
        <form id="kc-form-login" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
          {!usernameHidden ? (
            <Field
              id="username"
              label={!realm.loginWithEmailAllowed ? msg("username") : !realm.registrationEmailAsUsername ? msg("usernameOrEmail") : msg("email")}
            >
              <input
                id="username"
                name="username"
                type="text"
                inputMode="email"
                defaultValue={login.username ?? ""}
                placeholder={msgStr("cinxEmailPlaceholder")}
                autoFocus
                autoComplete="username"
                aria-invalid={invalid || undefined}
              />
            </Field>
          ) : null}
          <Field
            id="password"
            label={msg("password")}
            aside={realm.resetPasswordAllowed ? <a className="link" href={url.loginResetCredentialsUrl}>{msg("doForgotPassword")}</a> : null}
          >
            <PasswordInput
              id="password"
              name="password"
              autoComplete="current-password"
              autoFocus={usernameHidden}
              invalid={invalid}
              placeholder={msgStr("cinxPasswordPlaceholder")}
              showLabel={msgStr("showPassword")}
              hideLabel={msgStr("hidePassword")}
            />
          </Field>
          {realm.rememberMe && !usernameHidden ? (
            <Checkbox id="rememberMe" name="rememberMe" label={msg("rememberMe")} defaultChecked={!!login.rememberMe} />
          ) : null}
          <input type="hidden" id="id-hidden-input" name="credentialId" defaultValue={auth.selectedCredential} />
          <Submit id="kc-login" name="login" label={msgStr("doLogIn")} busyLabel={msg("cinxSigningIn")} busy={busy} />
        </form>
      ) : null}
      {linking && !whoShown ? (
        <a className="link back" href={url.loginRestartFlowUrl}>
          <ArrowLeft aria-hidden />
          {msg("cinxCancelLinking")}
        </a>
      ) : null}
      {realm.password && realm.registrationAllowed && !registrationDisabled && !admin ? (
        <p className="foot">
          {msg("noAccount")} <a className="link" href={url.registrationUrl}>{msg("cinxSignUpFree")}</a>
        </p>
      ) : null}
    </Template>
  );
}
