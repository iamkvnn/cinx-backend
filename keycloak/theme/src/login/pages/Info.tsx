import { CircleCheck } from "lucide-react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";

/** info.ftl: a finished action (email verified, password changed…) and the way back. */
export default function Info(props: PageProps<Extract<KcContext, { pageId: "info.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { advancedMsgStr, msg } = i18n;
  const { messageHeader, message, requiredActions, skipLink, pageRedirectUri, actionUri, client } = kcContext;

  const header = messageHeader ? advancedMsgStr(messageHeader) : message.summary;
  const actions = requiredActions?.map((action) => advancedMsgStr(`requiredAction.${action}`)).join(", ");
  // the summary is the title when there is no header, so the body then carries only the actions
  const body = [messageHeader ? message.summary?.trim() : "", actions ? `<b>${actions}</b>` : ""].filter(Boolean).join(" ") || undefined;
  const next = skipLink ? undefined : pageRedirectUri ? { href: pageRedirectUri, label: msg("backToApplication") } : actionUri ? { href: actionUri, label: msg("proceedWithAction") } : client.baseUrl ? { href: client.baseUrl, label: msg("backToApplication") } : undefined;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={false}
      mood={actionUri ? undefined : "success"}
      notice={{ icon: <CircleCheck aria-hidden />, tone: "ok" }}
      headerNode={<span dangerouslySetInnerHTML={{ __html: kcSanitize(header) }} />}
      subtitle={body ? <span dangerouslySetInnerHTML={{ __html: kcSanitize(body) }} /> : undefined}
    >
      {next ? (
        <a className="primary" href={next.href}>
          {next.label}
        </a>
      ) : null}
    </Template>
  );
}
