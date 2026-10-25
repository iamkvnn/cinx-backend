import { Suspense, lazy } from "react";
import type { ClassKey } from "keycloakify/login";
import DefaultPage from "keycloakify/login/DefaultPage";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";
import Template from "./Template";

const UserProfileFormFields = lazy(() => import("keycloakify/login/UserProfileFormFields"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const LoginResetPassword = lazy(() => import("./pages/LoginResetPassword"));
const LoginUpdatePassword = lazy(() => import("./pages/LoginUpdatePassword"));
const LoginVerifyEmail = lazy(() => import("./pages/LoginVerifyEmail"));
const LoginConfigTotp = lazy(() => import("./pages/LoginConfigTotp"));
const LoginOtp = lazy(() => import("./pages/LoginOtp"));
const IdpReviewUserProfile = lazy(() => import("./pages/IdpReviewUserProfile"));
const LoginIdpLinkConfirm = lazy(() => import("./pages/LoginIdpLinkConfirm"));
const LogoutConfirm = lazy(() => import("./pages/LogoutConfirm"));
const Info = lazy(() => import("./pages/Info"));
const ErrorPage = lazy(() => import("./pages/Error"));

/** The twelve designed pages (ui-spec.md); every other Keycloak page keeps Keycloakify's markup inside cinx's template. */
export default function KcPage({ kcContext }: { kcContext: KcContext }) {
  const { i18n } = useI18n({ kcContext });
  const common = { i18n, Template, classes, doUseDefaultCss: false } as const;

  return (
    <Suspense>
      {(() => {
        switch (kcContext.pageId) {
          case "login.ftl":
            return <Login kcContext={kcContext} {...common} />;
          case "register.ftl":
            return <Register kcContext={kcContext} {...common} />;
          case "login-reset-password.ftl":
            return <LoginResetPassword kcContext={kcContext} {...common} />;
          case "login-update-password.ftl":
            return <LoginUpdatePassword kcContext={kcContext} {...common} />;
          case "login-verify-email.ftl":
            return <LoginVerifyEmail kcContext={kcContext} {...common} />;
          case "login-config-totp.ftl":
            return <LoginConfigTotp kcContext={kcContext} {...common} />;
          case "login-otp.ftl":
            return <LoginOtp kcContext={kcContext} {...common} />;
          case "idp-review-user-profile.ftl":
            return <IdpReviewUserProfile kcContext={kcContext} {...common} />;
          case "login-idp-link-confirm.ftl":
            return <LoginIdpLinkConfirm kcContext={kcContext} {...common} />;
          case "logout-confirm.ftl":
            return <LogoutConfirm kcContext={kcContext} {...common} />;
          case "info.ftl":
            return <Info kcContext={kcContext} {...common} />;
          case "error.ftl":
            return <ErrorPage kcContext={kcContext} {...common} />;
          default:
            return <DefaultPage kcContext={kcContext} {...common} UserProfileFormFields={UserProfileFormFields} doMakeUserConfirmPassword={true} />;
        }
      })()}
    </Suspense>
  );
}

/** Keycloakify's class keys mapped onto cinx.css, for the pages that keep the default markup. */
const classes = {
  kcFormClass: "form",
  kcFormGroupClass: "kc-group",
  kcLabelClass: "kc-label",
  kcInputClass: "kc-input",
  kcInputErrorMessageClass: "err",
  kcInputGroup: "input-wrap",
  kcFormPasswordVisibilityButtonClass: "eye",
  kcFormPasswordVisibilityIconShow: "i-eye",
  kcFormPasswordVisibilityIconHide: "i-eye-off",
  kcCheckboxInputClass: "kc-checkbox",
  kcButtonClass: "kc-btn",
  kcButtonPrimaryClass: "is-primary",
  kcButtonDefaultClass: "is-ghost",
  kcButtonBlockClass: "is-block",
  kcFormButtonsClass: "kc-buttons",
  kcFormOptionsWrapperClass: "kc-options",
  kcFormSocialAccountListClass: "providers",
  kcFormSocialAccountListButtonClass: "ghost",
} satisfies { [key in ClassKey]?: string };
