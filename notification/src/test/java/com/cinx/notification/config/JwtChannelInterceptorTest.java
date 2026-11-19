package com.cinx.notification.config;

import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.security.CinxJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JwtChannelInterceptorTest {
    @Test
    void connectUsesTheSharedConverterAndRequiresTheReadPermission() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        CinxJwtAuthenticationConverter converter = mock(CinxJwtAuthenticationConverter.class);
        AbstractAuthenticationToken authentication = mock(AbstractAuthenticationToken.class);
        Jwt jwt = Jwt.withTokenValue("jwt").header("alg", "RS256").subject("user-1").build();
        when(decoder.decode("jwt")).thenReturn(jwt);
        when(converter.convert(jwt)).thenReturn(authentication);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(decoder, converter);

        when(authentication.getAuthorities()).thenReturn(List.of());
        assertThatThrownBy(() -> interceptor.preSend(connect(), mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(ForbiddenException.class);

        doReturn(List.of(new SimpleGrantedAuthority("PERM_notification.read.self"))).when(authentication).getAuthorities();
        var accepted = interceptor.preSend(connect(), mock(org.springframework.messaging.MessageChannel.class));
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(accepted);
        assertThat(headers.getUser()).isSameAs(authentication);
    }

    private org.springframework.messaging.Message<byte[]> connect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer jwt");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
