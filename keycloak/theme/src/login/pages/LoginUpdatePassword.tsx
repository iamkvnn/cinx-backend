import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";
import { Checkbox, Field, PasswordInput } from "../ui/Field";

/** login-update-password.ftl: from the app's "change password" (with Huỷ) or forced by Keycloak (without). */
export default function LoginUpdatePassword(props: PageProps<Extract<KcContext, { pageId: "login-update-password.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, messagesPerField, isAppInitiatedAction } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const error = (name: string) => (messagesPerField.existsError(name) ? messagesPerField.get(name) : undefined);
  const invalid = messagesPerField.existsError("password", "password-confirm");

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={!invalid}
      headerNode={msg("updatePasswordTitle")}
      subtitle={msg("cinxUpdatePasswordSubtitle")}
    >
      <form id="kc-passwd-update-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        <Field id="password-new" label={msg("passwordNew")} error={error("password")}>
          <PasswordInput
            id="password-new"
            name="password-new"
            autoComplete="new-password"
            autoFocus
            invalid={invalid}
            placeholder={msgStr("cinxNewPasswordPlaceholder")}
            showLabel={msgStr("showPassword")}
            hideLabel={msgStr("hidePassword")}
          />
        </Field>
        <Field id="password-confirm" label={msg("passwordConfirm")} error={error("password-confirm")}>
          <PasswordInput
            id="password-confirm"
            name="password-confirm"
            autoComplete="new-password"
            invalid={invalid}
            placeholder={msgStr("cinxConfirmPasswordPlaceholder")}
            showLabel={msgStr("showPassword")}
            hideLabel={msgStr("hidePassword")}
          />
        </Field>
        <Checkbox id="logout-sessions" name="logout-sessions" value="on" defaultChecked label={msg("logoutOtherSessions")} />
        <Submit label={msgStr("cinxSavePassword")} busyLabel={msg("cinxSaving")} busy={busy} />
        {isAppInitiatedAction ? (
          <button type="submit" className="text-btn" name="cancel-aia" value="true" formNoValidate>
            {msg("doCancel")}
          </button>
        ) : null}
      </form>
    </Template>
  );
}
