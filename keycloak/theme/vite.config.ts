import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";

export default defineConfig({
  plugins: [
    react(),
    keycloakify({
      themeName: "cinx",
      accountThemeImplementation: "none",
      // Keycloak 26 loads the "all other versions" jar; the 22-25 one is not built
      keycloakVersionTargets: {
        "22-to-25": false,
        "all-other-versions": "cinx-keycloak-theme.jar",
      },
    }),
  ],
});
