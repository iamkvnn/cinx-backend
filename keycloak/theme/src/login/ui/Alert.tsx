import { CircleAlert, CircleCheck, Info, TriangleAlert } from "lucide-react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

const ICONS = { error: CircleAlert, warning: TriangleAlert, success: CircleCheck, info: Info };

/** Keycloak's page message (success · warning · error · info); its summary is HTML. */
export function Alert({ type, html }: { type: keyof typeof ICONS; html: string }) {
  const Icon = ICONS[type];
  return (
    <div className={`alert is-${type}`} role={type === "error" ? "alert" : "status"}>
      <Icon aria-hidden />
      <span dangerouslySetInnerHTML={{ __html: kcSanitize(html) }} />
    </div>
  );
}
