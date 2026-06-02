package com.student.bank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.AuthUser;
import com.student.bank.common.ErrorCode;
import com.student.bank.common.Result;
import com.student.bank.entity.BankUser;
import com.student.bank.mapper.BankUserMapper;
import com.student.bank.service.TokenBlacklistService;
import com.student.bank.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_API = Set.of(
            "/api/user/register",
            "/api/user/login"
    );

    private final JwtUtil jwtUtil;
    private final BankUserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null || tokenBlacklistService.contains(token)) {
            writeUnauthorized(response);
            return;
        }

        try {
            Map<String, Object> payload = jwtUtil.parse(token);
            Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
            BankUser user = userMapper.selectById(userId);
            if (user == null || !"NORMAL".equals(user.getStatus())) {
                writeUnauthorized(response);
                return;
            }
            Integer tokenVersion = ((Number) payload.getOrDefault("tokenVersion", -1)).intValue();
            if (!tokenVersion.equals(user.getTokenVersion())) {
                writeUnauthorized(response);
                return;
            }
            AuthContext.set(new AuthUser(user.getId(), user.getUserName(), user.getRole(), user.getTokenVersion()));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response);
        } finally {
            AuthContext.clear();
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.startsWith("/api/")
                || PUBLIC_API.contains(path)
                || path.startsWith("/actuator")
                || path.startsWith("/api/public/");
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return auth.substring(7).trim();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(ErrorCode.UNAUTHORIZED)));
    }
}
