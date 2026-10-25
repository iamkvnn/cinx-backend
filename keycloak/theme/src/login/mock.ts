import { createGetKcContextMock } from "keycloakify/login/KcContext";
import type { KcContextExtension, KcContextExtensionPerPage } from "./KcContext";
import { themeNames, kcEnvDefaults } from "../kc.gen";

const kcContextExtension: KcContextExtension = {
  themeName: themeNames[0],
  properties: { ...kcEnvDefaults },
};
const kcContextExtensionPerPage: KcContextExtensionPerPage = {
  "register.ftl": {
    social: { displayInfo: true, providers: [{ alias: "google", providerId: "google", displayName: "Google", loginUrl: "#", iconClasses: "" }] },
  },
};

/** A Keycloak context for `pnpm dev`: pick a page with `?page=login.ftl` (see main.tsx). */
export const { getKcContextMock } = createGetKcContextMock({
  kcContextExtension,
  kcContextExtensionPerPage,
  // as the cinx realm: Vietnamese and English, email as the username
  overrides: {
    locale: {
      currentLanguageTag: "vi",
      supported: [
        { languageTag: "vi", label: "Tiếng Việt", url: "?kc_locale=vi" },
        { languageTag: "en", label: "English", url: "?kc_locale=en" },
      ],
    },
    realm: { registrationEmailAsUsername: true, internationalizationEnabled: true },
  },
  overridesPerPage: {
    "login.ftl": { realm: { loginWithEmailAllowed: true, rememberMe: true } },
  },
});
