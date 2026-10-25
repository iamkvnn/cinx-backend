<#--
  This file has been claimed for ownership from @keycloakify/email-native version 260007.0.0.
  To relinquish ownership and restore this file to its original content, run the following command:

  $ npx keycloakify own --path "email/html/template.ftl" --revert

  cinx: the approved email design (claude-dev/2026-10-09-keycloak-theme/preview/emails.html). Tables and inline styles
  for mail clients; no images, since the email theme has no public URL for them and data URIs are blocked by Gmail.
-->

<#assign font = "'Plus Jakarta Sans','Segoe UI',Roboto,Helvetica,Arial,sans-serif">

<#-- the stock templates call this without arguments, so every parameter has a default -->
<#macro emailLayout title="" cta="" link="" note="">
<!doctype html>
<html lang="${(locale.language)!'vi'}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title?has_content?then(title, "cinx")}</title>
</head>
<body style="margin:0;padding:0;background-color:#f5f5fb;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f5fb;">
  <tr>
    <td align="center" style="padding:32px 16px 40px;">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;">
        <tr>
          <td align="center" style="padding-bottom:20px;font-family:${font?no_esc};font-size:24px;font-weight:800;letter-spacing:-0.04em;color:#4338ca;">cinx</td>
        </tr>
        <tr>
          <td align="center" style="background-color:#ffffff;border-radius:20px;padding:36px 40px;font-family:${font?no_esc};font-size:15px;line-height:1.6;color:#51506e;">
            <#if title?has_content>
            <h1 style="margin:0 0 10px;font-family:${font?no_esc};font-size:24px;line-height:1.25;font-weight:800;color:#14132b;">${title}</h1>
            </#if>
            <#nested>
            <#if cta?has_content && link?has_content>
            <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:6px auto 0;">
              <tr>
                <td style="border-radius:12px;background-color:#4338ca;border-bottom:4px solid #2f2791;">
                  <a href="${link}" style="display:inline-block;padding:15px 28px;font-family:${font?no_esc};font-size:16px;line-height:1;font-weight:800;color:#ffffff;text-decoration:none;">${cta}</a>
                </td>
              </tr>
            </table>
            </#if>
            <#if note?has_content>
            <p style="margin:14px 0 0;font-family:${font?no_esc};font-size:13px;line-height:1.5;color:#6e6d8a;">${note}</p>
            </#if>
            <#if link?has_content>
            <div style="height:1px;background-color:#e4e4f0;margin:26px 0 18px;line-height:1px;font-size:1px;">&nbsp;</div>
            <p style="margin:0;text-align:left;font-family:${font?no_esc};font-size:13px;line-height:1.6;color:#6e6d8a;">${msg("cinxFallback")}<br><a href="${link}" style="color:#4338ca;word-break:break-all;">${link}</a></p>
            </#if>
          </td>
        </tr>
        <tr>
          <td align="center" style="padding:22px 24px 0;font-family:${font?no_esc};font-size:12.5px;line-height:1.6;color:#6e6d8a;">
            ${msg("cinxFooter", (user.email)!"")}<br>${msg("cinxTagline")}
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
</#macro>

<#-- the paragraph under the title -->
<#macro paragraph>
<p style="margin:0 0 22px;font-family:${font?no_esc};font-size:15.5px;line-height:1.6;color:#51506e;"><#nested></p>
</#macro>

<#-- "Minh Anh", or "bạn" when the account has no first name -->
<#function greetName>
  <#return ((user.firstName)!"")?has_content?then(user.firstName, msg("cinxYou"))>
</#function>
