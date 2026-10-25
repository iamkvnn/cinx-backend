import { ArrowLeft, MailCheck, RotateCw } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";

/** login-verify-email.ftl: the "check your inbox" notice; the resend link posts back to Keycloak. */
export default function LoginVerifyEmail(props: PageProps<Extract<KcContext, { pageId: "login-verify-email.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, user } = kcContext;
  const { msg } = i18n;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      headerNode={msg("emailVerifyTitle")}
      notice={{ icon: <MailCheck aria-hidden /> }}
      subtitle={msg("emailVerifyInstruction1", user?.email ?? "")}
    >
      <p className="hint">{msg("cinxSpamHint")}</p>
      <a className="ghost" href={url.loginAction}>
        <RotateCw aria-hidden />
        {msg("cinxResend")}
      </a>
      <a className="link back" href={url.loginRestartFlowUrl}>
        <ArrowLeft aria-hidden />
        {msg("restartLoginTooltip")}
      </a>
    </Template>
  );
}
