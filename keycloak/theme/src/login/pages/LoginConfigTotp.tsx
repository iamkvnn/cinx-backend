import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";
import { Checkbox, Field } from "../ui/Field";

/** login-config-totp.ftl: install an app, scan (or type the key), enter the code; the wide card. */
export default function LoginConfigTotp(props: PageProps<Extract<KcContext, { pageId: "login-config-totp.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, isAppInitiatedAction, totp, mode, messagesPerField } = kcContext;
  const { msg, msgStr, advancedMsgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const error = (name: string) => (messagesPerField.existsError(name) ? messagesPerField.get(name) : undefined);
  const apps = totp.supportedApplications.map((app) => advancedMsgStr(app)).join(", ");
  const { policy } = totp;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      wide
      displayMessage={!messagesPerField.existsError("totp", "userLabel")}
      headerNode={msg("loginTotpTitle")}
    >
      <ol className="steps">
        <li>
          <span className="n">1</span>
          <p>{msg("cinxTotpStep1", apps)}</p>
        </li>
        <li>
          <span className="n">2</span>
          {mode === "manual" ? (
            <div>
              <p>{msg("cinxTotpManual")}</p>
              <code className="key" id="kc-totp-secret-key">{totp.totpSecretEncoded}</code>
              <p className="hint">
                {msg("loginTotpType")}: {msg(`loginTotp.${policy.type}`)} · {msg("loginTotpAlgorithm")}: {policy.getAlgorithmKey()} ·{" "}
                {msg("loginTotpDigits")}: {policy.digits}
                {policy.type === "totp" ? <> · {msg("loginTotpInterval")}: {policy.period}</> : <> · {msg("loginTotpCounter")}: {policy.initialCounter}</>}
              </p>
              <a className="link" href={totp.qrUrl} id="mode-barcode">{msg("loginTotpScanBarcode")}</a>
            </div>
          ) : (
            <div>
              <p>{msg("cinxTotpScan")}</p>
              <div className="qr">
                <img id="kc-totp-secret-qr-code" src={`data:image/png;base64, ${totp.totpSecretQrCode}`} alt={msgStr("cinxTotpQrAlt")} width={132} height={132} />
                <p className="hint">
                  {msg("cinxTotpCantScan")}
                  <br />
                  <a className="link" href={totp.manualUrl} id="mode-manual">{msg("loginTotpUnableToScan")}</a>
                </p>
              </div>
            </div>
          )}
        </li>
        <li>
          <span className="n">3</span>
          <p>{msg("cinxTotpStep3", String(policy.digits))}</p>
        </li>
      </ol>
      <form id="kc-totp-settings-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        <div className="pair">
          <Field id="totp" label={msg("authenticatorCode")} error={error("totp")}>
            <input
              id="totp"
              name="totp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder={msgStr("cinxCodePlaceholder")}
              aria-invalid={!!error("totp") || undefined}
              aria-describedby={error("totp") ? "totp-error" : undefined}
            />
          </Field>
          <Field
            id="userLabel"
            label={msg("loginTotpDeviceName")}
            optional={totp.otpCredentials.length >= 1 ? undefined : msg("cinxOptional")}
            error={error("userLabel")}
          >
            <input
              id="userLabel"
              name="userLabel"
              type="text"
              autoComplete="off"
              placeholder={msgStr("cinxDevicePlaceholder")}
              aria-invalid={!!error("userLabel") || undefined}
            />
          </Field>
        </div>
        <input type="hidden" id="totpSecret" name="totpSecret" value={totp.totpSecret} />
        {mode ? <input type="hidden" id="mode" value={mode} /> : null}
        <Checkbox id="logout-sessions" name="logout-sessions" value="on" defaultChecked label={msg("logoutOtherSessions")} />
        <Submit id="saveTOTPBtn" label={msgStr("cinxTotpSubmit")} busyLabel={msg("cinxTurningOn")} busy={busy} />
        {isAppInitiatedAction ? (
          <button type="submit" className="text-btn" id="cancelTOTPBtn" name="cancel-aia" value="true" formNoValidate>
            {msg("doCancel")}
          </button>
        ) : null}
      </form>
    </Template>
  );
}
