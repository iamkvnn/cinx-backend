import { Link } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";

/** login-idp-link-confirm.ftl: the Google email already has a cinx account; link it, or review the details. */
export default function LoginIdpLinkConfirm(props: PageProps<Extract<KcContext, { pageId: "login-idp-link-confirm.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, idpAlias } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();
  const idp = idpAlias.charAt(0).toUpperCase() + idpAlias.slice(1);

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      headerNode={msg("confirmLinkIdpTitle")}
      notice={{ icon: <Link aria-hidden /> }}
      subtitle={msg("cinxLinkText", idp)}
    >
      <form id="kc-register-form" className="form" action={url.loginAction} method="post" onSubmit={onSubmit}>
        <Submit id="linkAccount" name="submitAction" value="linkAccount" label={msgStr("confirmLinkIdpContinue", idp)} busyLabel={msg("cinxRedirecting")} busy={busy} />
        <button type="submit" className="ghost" id="updateProfile" name="submitAction" value="updateProfile">
          {msg("confirmLinkIdpReviewProfile")}
        </button>
      </form>
    </Template>
  );
}
