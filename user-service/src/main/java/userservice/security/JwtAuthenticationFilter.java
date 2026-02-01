package userservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import userservice.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String xUserId = request.getHeader("X-User-Id");
        String xUserRole = request.getHeader("X-User-Role");

        if (xUserId != null && xUserRole != null) {
            try {
                Long userId = Long.parseLong(xUserId);
                String formattedRole = xUserRole.startsWith("ROLE_") ? xUserRole : "ROLE_" + xUserRole;

                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(formattedRole));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        } else {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                log.info("Request path: {}", request.getRequestURI());
                String token = header.substring(7);
                try {
                    Long userId = jwtProvider.getUserIdFromToken(token);
                    String role = jwtProvider.getRoleFromToken(token);
                    String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    Optional.ofNullable(userId).ifPresent(u -> {
                        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(formattedRole));
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                } catch (JwtException ex) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
