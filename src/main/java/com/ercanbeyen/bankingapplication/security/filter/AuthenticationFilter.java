package com.ercanbeyen.bankingapplication.security.filter;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.service.UserRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class AuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRevocationService userRevocationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = JwtUtil.extractToken(request).orElse(null);

            if (Optional.ofNullable(token).isPresent() && jwtService.validateToken(token)) {
                String username = jwtService.extractSubject(token);
                long issuedAt = jwtService.extractIssuedAt(token).getTime();

                if (userRevocationService.isTokenRevoked(username, issuedAt)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(String.format(ResponseMessage.ACCESS_DENIED, "For security reasons, all sessions for this account have been closed!"));
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception exception) {
            log.error("Error occurred, while processing token: {}", exception.getMessage());
            response.setHeader("error", exception.getMessage());
            response.sendError(HttpStatus.FORBIDDEN.value());
        }

        filterChain.doFilter(request, response);
    }
}

