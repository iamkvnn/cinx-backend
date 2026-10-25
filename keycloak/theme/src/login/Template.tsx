import { useEffect, type ReactNode } from "react";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { KcContext } from "./KcContext";
import type { I18n } from "./i18n";
import { useSky } from "./sky/useSky";
import { ClockChip, Island, LangSwitch, LearnMark, Sky } from "./shell/Scene";
import { Alert } from "./ui/Alert";
import type { OwlMood } from "./owl/CinxOwl";
import "@fontsource-variable/plus-jakarta-sans";
import "./cinx.css";

export type CinxTemplateProps = TemplateProps<KcContext, I18n> & {
  /** a line under the title */
  subtitle?: ReactNode;
  /** a centred notice card under this icon tile */
  notice?: { icon: ReactNode; tone?: "ok" | "bad" };
  /** the 520px card (two-step setup) */
  wide?: boolean;
  /** the owl's reaction when the page opens; by default the page message's type */
  mood?: OwlMood;
};

/** The greeting copy beside the island, by page (keys `cinxGreet<Page>Title` / `Lead` in i18n.ts). */
const GREETING: Partial<Record<KcContext["pageId"], string>> = {
  "login.ftl": "Login",
  "register.ftl": "Register",
  "login-reset-password.ftl": "Reset",
  "login-update-password.ftl": "UpdatePassword",
  "login-verify-email.ftl": "VerifyEmail",
  "login-idp-link-email.ftl": "VerifyEmail",
  "login-config-totp.ftl": "Totp",
  "login-otp.ftl": "Otp",
  "idp-review-user-profile.ftl": "GoogleProfile",
  "login-idp-link-confirm.ftl": "GoogleLink",
  "logout-confirm.ftl": "Logout",
  "info.ftl": "Info",
  "error.ftl": "Error",
};

const ADMIN_PAGES = ["Login", "Otp", "Totp"];

/** Whether the sign-in is for cinx-fe-admin (Keycloak client `cinx-admin`). */
export function isAdminClient(kcContext: KcContext) {
  return kcContext.client?.clientId === "cinx-admin";
}

/**
 * Every Keycloak page in cinx's dress (layout A of the approved preview): the sky of the visitor's hour, the island with
 * the Rive owl and the page's greeting on the left, the form on a frosted card on the right. Pages without a design of
 * their own come through Keycloakify's DefaultPage with this template.
 */
export default function Template(props: CinxTemplateProps) {
  const { displayInfo = false, displayMessage = true, headerNode, socialProvidersNode = null, infoNode = null, documentTitle } = props;
  const { kcContext, i18n, children, subtitle, notice, wide = false, mood } = props;
  const { msg, msgStr, advancedMsg, advancedMsgStr, currentLanguage, enabledLanguages } = i18n;
  const { realm, auth, url, message, isAppInitiatedAction, pageId } = kcContext;
  const sky = useSky();

  useEffect(() => {
    document.title = documentTitle ?? msgStr("loginTitle", realm.displayName || realm.name);
  }, [documentTitle, msgStr, realm.displayName, realm.name]);

  useEffect(() => {
    document.documentElement.lang = currentLanguage.languageTag;
  }, [currentLanguage.languageTag]);

  const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss: false });
  if (!isReadyToRender) return null;

  // cinx-admin (the operators' app) has its own copy on the pages it reaches
  const admin = isAdminClient(kcContext) && ADMIN_PAGES.includes(GREETING[pageId] ?? "") ? "Admin" : "";
  const page = admin + (GREETING[pageId] ?? "Default");
  const greet = page === "Login" && sky.greeting === "night" ? "LoginNight" : page;
  const showMessage = displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction);
  const owlMood = mood ?? (message?.type === "error" ? "error" : message?.type === "success" ? "success" : undefined);
  const who = auth !== undefined && auth.showUsername && !auth.showResetCredentials;

  return (
    <div className="kc" data-palette={sky.palette}>
      <Sky sky={sky} />
      <Island sky={sky} mood={owlMood} />
      <header className="top">
        <LearnMark />
        {enabledLanguages.length > 1 ? (
          <LangSwitch languages={enabledLanguages} current={currentLanguage.languageTag} label={msgStr("languages")} />
        ) : null}
      </header>
      <div className={wide ? "stage is-wide" : "stage"}>
        <section className="greet" aria-hidden>
          <ClockChip sky={sky} time={sky.time} label={admin ? msgStr("cinxAdminChip") : advancedMsgStr(`cinxSky_${sky.greeting}`)} />
          <h2>{advancedMsg(`cinxGreet${greet}Title`)}</h2>
          <p>{advancedMsg(`cinxGreet${page}Lead`)}</p>
        </section>
        <main className={notice ? "card is-notice" : "card"}>
          <header className="card-head">
            {notice ? <span className={notice.tone ? `tile is-${notice.tone}` : "tile"}>{notice.icon}</span> : null}
            <h1 id="kc-page-title">{headerNode}</h1>
            {subtitle ? <p className="sub">{subtitle}</p> : null}
            {who ? (
              <p className="who">
                <b>{auth.attemptedUsername}</b>
                <a href={url.loginRestartFlowUrl}>{msg("restartLoginTooltip")}</a>
              </p>
            ) : null}
          </header>
          {showMessage ? <Alert type={message.type} html={message.summary} /> : null}
          {children}
          {auth !== undefined && auth.showTryAnotherWayLink ? (
            <form id="kc-select-try-another-way-form" action={url.loginAction} method="post" className="try-another">
              <input type="hidden" name="tryAnotherWay" value="on" />
              <button type="submit" className="link">{msg("doTryAnotherWay")}</button>
            </form>
          ) : null}
          {socialProvidersNode}
          {displayInfo ? <div className="kc-info">{infoNode}</div> : null}
        </main>
      </div>
    </div>
  );
}
