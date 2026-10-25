<#--
  This file has been claimed for ownership from @keycloakify/email-native version 260007.0.0.
  To relinquish ownership and restore this file to its original content, run the following command:

  $ npx keycloakify own --path "email/html/password-reset.ftl" --revert
-->

<#import "template.ftl" as layout>
<@layout.emailLayout title=msg("cinxResetTitle") cta=msg("cinxResetCta") link=link note=msg("cinxResetExpiry", linkExpirationFormatter(linkExpiration))>
<@layout.paragraph>${kcSanitize(msg("cinxResetText", layout.greetName(), (user.email)!""))?no_esc}</@layout.paragraph>
</@layout.emailLayout>
