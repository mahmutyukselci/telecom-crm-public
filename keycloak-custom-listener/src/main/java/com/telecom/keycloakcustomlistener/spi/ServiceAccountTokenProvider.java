package com.telecom.keycloakcustomlistener.spi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceAccountTokenProvider {

    private static final Logger log = Logger.getLogger(ServiceAccountTokenProvider.class.getName());

    // NOTE: This URL is Keycloak container's INTERNAL port (8080),
    // not the external 8081 you see from outside (docker-compose port mapping) —
    // because this request is made from inside Keycloak to itself.
    private static final String TOKEN_URL = System.getenv().getOrDefault(
            "KEYCLOAK_INTERNAL_TOKEN_URL",
            "http://localhost:8080/realms/telecom-realm/protocol/openid-connect/token");

    private static final String CLIENT_ID = System.getenv().getOrDefault("SYNC_CLIENT_ID", "internal-sync-client");
    private static final String CLIENT_SECRET = System.getenv().getOrDefault("SYNC_CLIENT_SECRET", "");

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRES_IN_PATTERN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    private record CachedToken(String token, Instant expiresAt) {}

    public String getAccessToken() {
        CachedToken current = cache.get();
        if (current != null && Instant.now().isBefore(current.expiresAt().minusSeconds(10))) {
            return current.token();
        }
        return fetchNewToken();
    }

    private synchronized String fetchNewToken() {
        CachedToken current = cache.get();
        if (current != null && Instant.now().isBefore(current.expiresAt().minusSeconds(10))) {
            return current.token(); // başka thread zaten yenilemiş
        }
        try {
            String form = "grant_type=client_credentials&client_id=" + CLIENT_ID
                    + "&client_secret=" + CLIENT_SECRET;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.severe("Token request failed, status=" + response.statusCode() + " body=" + response.body());
                return null;
            }

            Matcher tokenMatcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
            Matcher expiryMatcher = EXPIRES_IN_PATTERN.matcher(response.body());

            if (tokenMatcher.find() && expiryMatcher.find()) {
                String token = tokenMatcher.group(1);
                long expiresIn = Long.parseLong(expiryMatcher.group(1));
                cache.set(new CachedToken(token, Instant.now().plusSeconds(expiresIn)));
                return token;
            }
            log.severe("Could not parse access_token from response");
            return null;

        } catch (Exception e) {
            log.severe("Failed to fetch service account token: " + e.getMessage());
            return null;
        }
    }
}