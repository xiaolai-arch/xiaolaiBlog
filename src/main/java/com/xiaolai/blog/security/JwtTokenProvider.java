package com.xiaolai.blog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** application.yml 里的兜底密钥：仅限本地开发；生产必须通过 JWT_SECRET 环境变量覆盖。 */
    static final String DEFAULT_DEV_SECRET = "xiaolai-blog-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expiration,
                            Environment environment) {
        if (DEFAULT_DEV_SECRET.equals(secret) && environment.matchesProfiles("prod", "production")) {
            throw new IllegalStateException("Production requires an independent JWT_SECRET; the development default is forbidden");
        }
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            log.warn("================================================================");
            log.warn("JWT 正在使用代码库内置的默认密钥！任何拿到源码的人都能伪造管理员");
            log.warn("token。生产环境请设置环境变量 JWT_SECRET 为随机长密钥后重启。");
            log.warn("================================================================");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String username, String role, long tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("version", tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    public Long getVersionFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
                .getPayload().get("version", Long.class);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        Object role = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
        return role != null ? role.toString() : "user";
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
