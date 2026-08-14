package com.example.agenticurl.controller;

import com.example.agenticurl.config.GovernanceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AgentControlTokenFilter extends OncePerRequestFilter {
    private final GovernanceProperties governance;

    public AgentControlTokenFilter(GovernanceProperties governance) {
        this.governance = governance;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/v1/agent-runs")) {
            String token = request.getHeader("X-Agent-Control-Token");
            if (token == null || !token.equals(governance.controlToken())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"title\":\"Unauthorized\",\"detail\":\"Valid X-Agent-Control-Token is required\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
