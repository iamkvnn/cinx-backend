import { Lock } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";
import { Field } from "../ui/Field";

/** idp-review-user-profile.ftl: the first Google sign-in confirms the name it brought (same fields as Register). */
export default function IdpReviewUserProfile(props: PageProps<Extract<KcContext, { pageId: "idp-review-user-profile.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, messagesPerField, profile } = kcContext;
  const { msg, msgStr, advancedMsg } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const attribute = (name: string) => profile.attributesByName[name];
  const error = (name: string) => (messagesPerField.existsError(name) ? messagesPerField.get(name) : undefined);

  const text = (name: "lastName" | "firstName", label: string, autoComplete: string) => (
    <Field id={name} label={advancedMsg(attribute(name)?.displayName ?? label)} error={error(name)}>
      <input
        id={name}
        name={name}
        type="text"
        defaultValue={attribute(name)?.value ?? ""}
        autoComplete={autoComplete}
        aria-invalid={!!error(name) || undefined}
        aria-describedby={error(name) ? `${name}-error` : undefined}
      />
    </Field>
  );
  const email = attribute("email");

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={messagesPerField.exists("global")}
      headerNode={msg("loginIdpReviewProfileTitle")}
      subtitle={msg("cinxGoogleProfileSubtitle")}
    >
      <form id="kc-idp-review-profile-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        {email ? (
          <Field id="email" label={advancedMsg(email.displayName ?? "${email}")} error={error("email")}>
            {email.readOnly ? (
              <div className="input-wrap is-locked">
                <input id="email" name="email" type="email" defaultValue={email.value ?? ""} readOnly />
                <Lock aria-hidden />
              </div>
            ) : (
              <input id="email" name="email" type="email" defaultValue={email.value ?? ""} autoComplete="email" aria-invalid={!!error("email") || undefined} />
            )}
          </Field>
        ) : null}
        <div className="pair">
          {text("lastName", "${lastName}", "family-name")}
          {text("firstName", "${firstName}", "given-name")}
        </div>
        <Submit label={msgStr("doContinue")} busyLabel={msg("cinxSaving")} busy={busy} />
      </form>
    </Template>
  );
}
