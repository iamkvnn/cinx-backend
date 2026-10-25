<#--
  This file has been claimed for ownership from @keycloakify/email-native version 260007.0.0.
  To relinquish ownership and restore this file to its original content, run the following command:

  $ npx keycloakify own --path "email/html/executeActions.ftl" --revert
-->

<#import "template.ftl" as layout>
<@layout.emailLayout title=msg("cinxActionsTitle") cta=msg("cinxActionsCta") link=link note=msg("cinxLinkExpiry", linkExpirationFormatter(linkExpiration))>
<@layout.paragraph>${kcSanitize(msg("cinxActionsText", layout.greetName()))?no_esc}</@layout.paragraph>
<#if requiredActions??>
<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:-8px auto 22px;">
  <#list requiredActions as action>
  <tr><td style="padding:4px 0;font-family:'Plus Jakarta Sans','Segoe UI',Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.4;font-weight:700;color:#14132b;">&#10003;&nbsp; ${msg("requiredAction.${action}")}</td></tr>
  </#list>
</table>
</#if>
</@layout.emailLayout>
