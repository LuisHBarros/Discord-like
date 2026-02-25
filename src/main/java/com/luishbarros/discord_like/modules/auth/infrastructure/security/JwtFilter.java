// auth/infrastructure/security/JwtFilter.java
package com.luishbarros.discord_like.modules.auth.infrastructure.security;

import com.luishbarros.discord_like.modules.auth.domain.model.User;
import com.luishbarros.discord_like.modules.auth.domain.model.error.InvalidTokenError;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.auth.domain.ports.TokenProvider;
import com.luishbarros.discord_like.modules.auth.domain.ports.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.NoSuchElementException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;

    public JwtFilter(TokenProvider tokenProvider, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && !tokenBlacklist.isBlacklisted(token)) {
            try {
                Long userId = tokenProvider.validateAccessToken(token);

                User user = userRepository.findById(userId).orElseThrow();
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                authenticatedUser.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenError | NoSuchElementException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or expired token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}