package com.cinx.common.security;

import com.cinx.common.exception.ProblemDetailResponseWriter;
import com.cinx.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class AuthorizationFreshnessFilter extends OncePerRequestFilter {
    private final AuthorizationFreshnessVerifier verifier;

    public AuthorizationFreshnessFilter(AuthorizationFreshnessVerifier verifier) { this.verifier = verifier; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Service tokens have no user state, and this servlet-wide filter also sees /internal/** calls.
        if (authentication instanceof JwtAuthenticationToken jwt
                && !"service".equals(jwt.getToken().getClaimAsString("token_kind"))
                && !verifier.isCurrent(jwt.getToken())) {
            ProblemDetailResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    ErrorCode.TOKEN_AUTHORIZATION_STALE, "Token authorization state is stale or inactive");
            return;
        }
        chain.doFilter(request, response);
    }
}
