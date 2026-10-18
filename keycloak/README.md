# CINX Keycloak

The tracked realm file contains no production secrets. It bootstraps the local
`cinx` realm, roles and the public PKCE web client.

- Keycloak owns credentials, sessions and OAuth2/OIDC protocol endpoints.
- CINX business profiles remain in the `user` service.
- SMTP and Google identity-provider secrets must be configured at deployment
  time and must not be committed to the realm export.
- Realm changes must be exported, reviewed and versioned before promotion.

The `uid` claim is the stable identifier used by CINX services. Existing users
must keep their current CINX ID when they are imported or federated.

## Admin sign-in (`cinx-admin`)

cinx-fe-admin signs in through the public client `cinx-admin` (PKCE, `http://localhost:5173`). Its browser flow is
overridden with `admin-step-up`: the session cookie, else password (LoA 1) and then OTP (LoA 2). The client demands
`urn:cinx:loa:2` (`minimum.acr.value`), and the realm's `acr.loa.map` names the levels. The tokens carry the `acr`,
`amr` and `auth_time` that `CinxMfaVerifier` checks: `amr` gets `otp` from the OTP execution's
`default.reference.value`, and stays for 900 s (`default.reference.maxAge`). The flow is bound to this client only,
so learners keep the realm's default browser flow.

A realm that already exists does not pick these up from `cinx-realm.json`. Apply them once, and give a dev user an
operator role (the first OTP is enrolled at that user's next admin sign-in):

```bash
python3 keycloak/scripts/apply-admin-client.py
docker exec keycloak /opt/keycloak/bin/kcadm.sh add-roles -r cinx --uusername <email> --rolename PLATFORM_ADMIN
```

## Theme

`theme/` is the `cinx` login and email theme, a [Keycloakify](https://docs.keycloakify.dev) 11 project (React,
Vite, pnpm). The image build (`Dockerfile`) packs it into a jar and adds it to `/opt/keycloak/providers/`; the realm
selects it with `loginTheme`/`emailTheme` and speaks `vi` and `en`.

- The look follows cinx-web's landing: the sky photos, the island stills and `owl.riv` in `theme/src/assets/` are
  copied from cinx-web (`public/landing/sky`, `tools/landing-3d/island.py perch`, `public/rive`). A change there is
  copied here by hand.
- Vietnamese is not in Keycloak's message set: `theme/src/login/i18n.vi.ts` and
  `theme/src/email/messages/messages_vi.properties` carry all of it.
- Emails are FreeMarker templates from `@keycloakify/email-native`. Only the files claimed with
  `npx keycloakify own --path <file>` are versioned; the rest are copied in by `pnpm install` and gitignored.
- Keycloak sends no email until an SMTP server is configured on the realm.

Work on it:

```bash
cd keycloak/theme
pnpm install
pnpm dev --port 5181              # mock pages: http://localhost:5181/?page=register.ftl&t=21 (t = hour, dev only)
pnpm build-keycloak-theme         # needs mvn and JDK 17 or 21 → dist_keycloak/cinx-keycloak-theme.jar
npx keycloakify start-keycloak    # a throwaway Keycloak in Docker with the theme, for the real flows
```

`--import-realm` skips a realm that already exists, so a running `keycloakdb` does not pick up the theme keys from
`cinx-realm.json`. After `docker compose build keycloak && docker compose up -d keycloak`, set them once:

```bash
docker compose exec keycloak sh -c '
  /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master \
    --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" &&
  /opt/keycloak/bin/kcadm.sh update realms/cinx -s loginTheme=cinx -s emailTheme=cinx \
    -s internationalizationEnabled=true -s "supportedLocales=[\"vi\",\"en\"]" -s defaultLocale=vi'
```
