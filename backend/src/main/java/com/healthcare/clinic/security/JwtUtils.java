package com.healthcare.clinic.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret:defaultSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm!}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms:86400000}")
    private int jwtExpirationMs;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32) {
            log.warn("JWT_SECRET is missing or short. Using fallback development key.");
            jwtSecret = "defaultSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm!";
        }
    }

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username = "";
        Long userId = null;
        Long branchId = null;
        List<String> roles = authentication.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        if (principal instanceof UserPrincipal up) {
            username = up.getUsername();
            userId = up.getUserId();
            branchId = up.getBranchId();
        } else if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
            try {
                java.lang.reflect.Method getIdMethod = principal.getClass().getMethod("getId");
                userId = (Long) getIdMethod.invoke(principal);
                java.lang.reflect.Method getBranchIdMethod = principal.getClass().getMethod("getBranchId");
                branchId = (Long) getBranchIdMethod.invoke(principal);
            } catch (Exception ignored) {}
        } else {
            username = principal.toString();
        }

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("branchId", branchId)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).getSubject();
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
