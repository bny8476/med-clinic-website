package com.healthcare.clinic.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class GrowthServiceProxyFilter extends OncePerRequestFilter {

    @Value("${app.growth-service.url:https://subclinic-website.onrender.com}")
    private String growthServiceUrl;

    @Value("${app.gateway.secret:clinic-internal-secret-key-2026}")
    private String gatewaySecret;

    private HttpClient httpClient;

    private static final Set<String> PROXIED_PATH_PREFIXES = Set.of(
            "/api/ecommerce",
            "/api/marketing",
            "/api/engagement",
            "/api/ai",
            "/api/subscriptions",
            "/api/analytics",
            "/api/telemedicine",
            "/api/inventory",
            "/api/vendor",
            "/api/hr",
            "/api/ambulance"
    );

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/ai"
    );

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host"
    );

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(gatewaySecret)) {
            gatewaySecret = "clinic-internal-secret-key-2026";
            log.info("app.gateway.secret not specified; defaulting to clinic-internal-secret-key-2026");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        log.info("GrowthServiceProxyFilter initialized forwarding target: {}", growthServiceUrl);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : PROXIED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false; // Should filter / proxy
            }
        }
        return true; // Skip filter for core clinical requests
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Enforce JWT authentication check for non-public proxied path prefixes
        boolean isPublicPath = PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (!isPublicPath) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                log.warn("Unauthorized proxy attempt for path: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Full authentication is required to access this resource\"}");
                return;
            }
        }

        String queryString = request.getQueryString();
        String targetUrl = growthServiceUrl.replaceAll("/+$", "") + path + (queryString != null ? "?" + queryString : "");

        log.debug("Proxying request [{}] {} -> {}", request.getMethod(), path, targetUrl);

        try {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

            HttpRequest.BodyPublisher bodyPublisher;
            if (body.length > 0 && !"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(body);
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .method(request.getMethod(), bodyPublisher)
                    .header("X-Internal-Gateway-Secret", gatewaySecret);

            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                        Enumeration<String> headers = request.getHeaders(headerName);
                        while (headers.hasMoreElements()) {
                            String headerValue = headers.nextElement();
                            builder.header(headerName, headerValue);
                        }
                    }
                }
            }

            HttpRequest proxiedRequest = builder.build();
            HttpResponse<InputStream> proxiedResponse = httpClient.send(proxiedRequest, HttpResponse.BodyHandlers.ofInputStream());

            response.setStatus(proxiedResponse.statusCode());

            proxiedResponse.headers().map().forEach((headerName, headerValues) -> {
                if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                    for (String headerValue : headerValues) {
                        response.addHeader(headerName, headerValue);
                    }
                }
            });

            try (InputStream is = proxiedResponse.body()) {
                if (is != null) {
                    StreamUtils.copy(is, response.getOutputStream());
                }
            }
            response.getOutputStream().flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Proxy request interrupted for path {}: {}", path, e.getMessage());
            response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Gateway request interrupted");
        } catch (Exception e) {
            log.error("Error proxying request to Growth Service for path {}: {}", path, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Growth Service unreachable\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
