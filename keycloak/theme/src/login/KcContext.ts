import type { ExtendKcContext } from "keycloakify/login";
import type { KcContext as KcContextBase } from "keycloakify/login/KcContext";
import type { KcEnvName, ThemeName } from "../kc.gen";

export type KcContextExtension = {
  themeName: ThemeName;
  properties: Record<KcEnvName, string> & {};
};

export type KcContextExtensionPerPage = {
  // Keycloak sets the identity providers on every page; Keycloakify types them on the login pages only
  "register.ftl": { social?: KcContextBase.Login["social"] };
};

export type KcContext = ExtendKcContext<KcContextExtension, KcContextExtensionPerPage>;
