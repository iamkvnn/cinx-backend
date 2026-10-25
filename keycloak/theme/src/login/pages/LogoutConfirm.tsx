import { ArrowLeft, LogOut } from "lucide-react";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";
import { Submit, useSubmitting } from "../ui/Buttons";

/** logout-confirm.ftl: shown when a logout arrives without a valid id_token_hint. */
export default function LogoutConfirm(props: PageProps<Extract<KcContext, { pageId: "logout-confirm.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { url, client, logoutConfirm } = kcContext;
  const { msg, msgStr } = i18n;
  const { busy, onSubmit } = useSubmitting();

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      headerNode={msg("logoutConfirmTitle")}
      notice={{ icon: <LogOut aria-hidden /> }}
      subtitle={msg("logoutConfirmHeader")}
    >
      <form className="form" action={url.logoutConfirmAction} method="POST" onSubmit={onSubmit}>
        <input type="hidden" name="session_code" value={logoutConfirm.code} />
        <Submit id="kc-logout" name="confirmLogout" label={msgStr("doLogout")} busyLabel={msg("cinxSigningOut")} busy={busy} />
      </form>
      {!logoutConfirm.skipLink && client.baseUrl ? (
        <a className="link back" href={client.baseUrl}>
          <ArrowLeft aria-hidden />
          {msg("backToApplication")}
        </a>
      ) : null}
    </Template>
  );
}
