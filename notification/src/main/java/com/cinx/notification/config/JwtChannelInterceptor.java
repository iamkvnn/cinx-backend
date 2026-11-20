package com.cinx.notification.config;

import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.UnauthorizedException;
import com.cinx.common.security.CinxJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final CinxJwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            throw new UnauthorizedException("STOMP security headers are missing");
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("A bearer token is required for STOMP CONNECT");
            }
            String token = authHeader.substring(7).trim();
            try {
                Jwt jwt = jwtDecoder.decode(token);
                AbstractAuthenticationToken auth = jwtAuthenticationConverter.convert(jwt);
                if (!hasAuthority(auth, "PERM_notification.read.self")) {
                    throw new ForbiddenException("Notification subscription permission is required");
                }
                accessor.setUser(auth);
            } catch (UnauthorizedException | ForbiddenException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new UnauthorizedException("Invalid bearer token");
            }
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            if (!(accessor.getUser() instanceof AbstractAuthenticationToken auth)
                    || !hasAuthority(auth, "PERM_notification.read.self")) {
                throw new UnauthorizedException("STOMP connection is not authenticated");
            }
            if (!Objects.equals("/user/queue/notifications", accessor.getDestination())) {
                throw new ForbiddenException("STOMP subscription destination is not allowed");
            }
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            throw new ForbiddenException("STOMP client send is not allowed");
        }
        return message;
    }

    private boolean hasAuthority(AbstractAuthenticationToken authentication, String expected) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expected.equals(authority.getAuthority()));
    }
}
