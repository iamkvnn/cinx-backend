import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { ProviderButtons, Submit, useSubmitting } from "../ui/Buttons";
import { Field, PasswordInput } from "../ui/Field";

type RegisterContext = Extract<KcContext, { pageId: "register.ftl" }>;

/**
 * register.ftl with the realm's fields spelled out (email as the username, last name before first name, password and
 * its confirmation). An attribute added to the realm's user profile later needs a field here too.
 */
export default function Register(props: PageProps<RegisterContext, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, messagesPerField, profile, passwordRequired, messageHeader, social } = kcContext;
  const { msg, msgStr, advancedMsg } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const value = (name: string) => profile.attributesByName[name]?.value ?? "";
  const error = (name: string) => (messagesPerField.existsError(name) ? messagesPerField.get(name) : undefined);

  // the email-taken error gets a way to sign in instead (the server renders it from the same bundle)
  const emailTaken = error("email") === msgStr("emailExistsMessage");

  const text = (name: "email" | "lastName" | "firstName", label: string, placeholder: string, autoComplete: string) => (
    <Field
      id={name}
      label={advancedMsg(profile.attributesByName[name]?.displayName ?? label)}
      error={error(name)}
      errorAction={name === "email" && emailTaken ? <a className="link" href={url.loginUrl}>{msg("doLogIn")}</a> : undefined}
    >
      <input
        id={name}
        name={name}
        type={name === "email" ? "email" : "text"}
        defaultValue={value(name)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        autoFocus={name === "email"}
        aria-invalid={!!error(name) || undefined}
        aria-describedby={error(name) ? `${name}-error` : undefined}
      />
    </Field>
  );

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      headerNode={messageHeader !== undefined ? advancedMsg(messageHeader) : msg("registerTitle")}
      subtitle={msg("cinxRegisterSubtitle")}
      displayMessage={messagesPerField.exists("global")}
    >
      <ProviderButtons providers={social?.providers} label={(name) => msgStr("cinxRegisterWith", name)} />
      {social?.providers?.length ? <div className="or">{msg("cinxOrEmail")}</div> : null}
      <form id="kc-register-form" className="form" action={url.registrationAction} method="post" onSubmit={onSubmit}>
        {text("email", "${email}", msgStr("cinxEmailPlaceholder"), "email")}
        <div className="pair">
          {text("lastName", "${lastName}", msgStr("cinxLastNamePlaceholder"), "family-name")}
          {text("firstName", "${firstName}", msgStr("cinxFirstNamePlaceholder"), "given-name")}
        </div>
        {passwordRequired ? (
          <>
            <Field id="password" label={msg("password")} error={error("password")}>
              <PasswordInput
                id="password"
                name="password"
                autoComplete="new-password"
                invalid={!!error("password")}
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
                invalid={!!error("password-confirm")}
                placeholder={msgStr("cinxConfirmPasswordPlaceholder")}
                showLabel={msgStr("showPassword")}
                hideLabel={msgStr("hidePassword")}
              />
            </Field>
          </>
        ) : null}
        <Submit label={msgStr("doRegister")} busyLabel={msg("cinxCreating")} busy={busy} />
      </form>
      <p className="foot">
        {msg("cinxHaveAccount")} <a className="link" href={url.loginUrl}>{msg("doLogIn")}</a>
      </p>
    </Template>
  );
}
