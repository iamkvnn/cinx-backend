import { Link2Off } from "lucide-react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";

/** error.ftl: what went wrong, and the way back to cinx (which starts a fresh sign-in). */
export default function Error(props: PageProps<Extract<KcContext, { pageId: "error.ftl" }>, I18n>) {
  const { kcContext, i18n, doUseDefaultCss, classes } = props;
  const { message, client, skipLink } = kcContext;
  const { msg } = i18n;

  return (
    <Template
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={false}
      mood="error"
      notice={{ icon: <Link2Off aria-hidden />, tone: "bad" }}
      headerNode={msg("errorTitle")}
      subtitle={<span dangerouslySetInnerHTML={{ __html: kcSanitize(message.summary) }} />}
    >
      {!skipLink && client?.baseUrl ? (
        <a className="primary" id="backToApplication" href={client.baseUrl}>
          {msg("backToApplication")}
        </a>
      ) : null}
    </Template>
  );
}
