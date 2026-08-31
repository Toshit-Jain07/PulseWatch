package com.toshit.PulseWatch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;



import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override               //not specifically req , just a saftey net
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Not a rejection
                                                     //The request is forwarded to whatever's next in the chain
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        if (jwtUtil.isTokenValid(token)) {
            String email = jwtUtil.extractEmail(token);
            Long userId = jwtUtil.extractUserId(token);

            var authToken = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.emptyList()
            );
            authToken.setDetails(userId);   //extra info

            SecurityContextHolder.getContext().setAuthentication(authToken); //Auth details attached to the thread
        }

        filterChain.doFilter(request, response);
    }
}