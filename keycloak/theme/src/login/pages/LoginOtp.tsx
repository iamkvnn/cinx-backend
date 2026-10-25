import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Alert } from "../ui/Alert";
import { Submit, useSubmitting } from "../ui/Buttons";
import { Field } from "../ui/Field";

/** login-otp.ftl: one large code field; a choice of device when the user has several. */
export default function LoginOtp(props: PageProps<Extract<KcContext, { pageId: "login-otp.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { otpLogin, url, messagesPerField } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const invalid = messagesPerField.existsError("totp");

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={!invalid}
      headerNode={msg("cinxOtpTitle")}
      subtitle={msg("cinxOtpSubtitle")}
    >
      {invalid ? <Alert type="error" html={messagesPerField.get("totp")} /> : null}
      <form id="kc-otp-login-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        {otpLogin.userOtpCredentials.length > 1 ? (
          <fieldset className="devices">
            {otpLogin.userOtpCredentials.map((credential, index) => (
              <label key={credential.id} className="check" htmlFor={`kc-otp-credential-${index}`}>
                <input
                  id={`kc-otp-credential-${index}`}
                  type="radio"
                  name="selectedCredentialId"
                  value={credential.id}
                  defaultChecked={credential.id === otpLogin.selectedCredentialId}
                />
                <span>{credential.userLabel}</span>
              </label>
            ))}
          </fieldset>
        ) : null}
        <Field id="otp" label={msg("loginOtpOneTime")}>
          <input
            id="otp"
            name="otp"
            type="text"
            className="code"
            inputMode="numeric"
            autoComplete="one-time-code"
            autoFocus
            placeholder="000000"
            aria-invalid={invalid || undefined}
          />
        </Field>
        <Submit id="kc-login" name="login" label={msgStr("cinxConfirm")} busyLabel={msg("cinxChecking")} busy={busy} />
      </form>
    </Template>
  );
}
