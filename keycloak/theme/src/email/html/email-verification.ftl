<#--
  This file has been claimed for ownership from @keycloakify/email-native version 260007.0.0.
  To relinquish ownership and restore this file to its original content, run the following command:

  $ npx keycloakify own --path "email/html/email-verification.ftl" --revert
-->

<#import "template.ftl" as layout>
<@layout.emailLayout title=msg("cinxVerifyTitle") cta=msg("cinxVerifyCta") link=link note=msg("cinxLinkExpiry", linkExpirationFormatter(linkExpiration))>
<@layout.paragraph>${kcSanitize(msg("cinxVerifyText", layout.greetName(), (user.email)!""))?no_esc}</@layout.paragraph>
</@layout.emailLayout>
