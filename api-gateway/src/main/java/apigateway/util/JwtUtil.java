package apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Mono<Claims> validateTokenReactive(String token) {
        return Mono.fromCallable(() -> {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            Claims claims = jws.getBody();
            Date exp = claims.getExpiration();
            if (exp != null && exp.before(new Date())) {
                throw new JwtException("Token expired");
            }
            return claims;
        });
    }

    public String generateToken(Long userId, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
