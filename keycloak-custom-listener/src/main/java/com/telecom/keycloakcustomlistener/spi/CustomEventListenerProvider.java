package com.telecom.keycloakcustomlistener.spi;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public class CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class.getName());

    private static final String GATEWAY_URL =
            System.getenv().getOrDefault("GATEWAY_URL", "http://host.docker.internal:8080");

    private final ServiceAccountTokenProvider tokenProvider = new ServiceAccountTokenProvider();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    private final KeycloakSession session;

    @Override
    public void onEvent(Event event) {
        EventType type = event.getType();

        if (type == EventType.REGISTER || type == EventType.UPDATE_PROFILE) {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user == null) {
                log.warning("User not found for sync, userId=" + event.getUserId());
                return;
            }
            String phone = user.getFirstAttribute("phone");

            String body = String.format(
                    "{\"keycloakUserId\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}",
                    escape(user.getId()),
                    escape(user.getFirstName()),
                    escape(user.getLastName()),
                    escape(user.getEmail()),
                    escape(phone)
            );
            sendRequest("PUT", "/api/v1/customers/webhook", body, event.getUserId());

        } else if (type == EventType.DELETE_ACCOUNT) {
            sendDelete(event.getUserId());
        }
    }

    private void sendDelete(String userId) {
        String token = tokenProvider.getAccessToken();
        if (token == null) {
            log.severe("Skipping delete sync for userId=" + userId + " — could not obtain access token");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/api/v1/customers/webhook/" + userId))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .timeout(Duration.ofSeconds(5))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> log.info("DELETE sync userId=" + userId + " -> HTTP " + response.statusCode()))
                .exceptionally(ex -> {
                    log.severe("DELETE sync FAILED userId=" + userId + ": " + ex.getMessage());
                    return null;
                });
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {}

    private void sendRequest(String method, String path, String body, String userId) {
        String token = tokenProvider.getAccessToken();
        if (token == null) {
            log.severe("Skipping sync for userId=" + userId + " — could not obtain access token");
            return;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(5));

        HttpRequest request = switch (method) {
            case "PUT" -> builder.method("PUT", HttpRequest.BodyPublishers.ofString(body)).build();
            case "DELETE" -> builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body)).build();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> log.info(method + " sync userId=" + userId + " -> HTTP " + response.statusCode()))
                .exceptionally(ex -> {
                    log.severe(method + " sync FAILED userId=" + userId + ": " + ex.getMessage());
                    return null;
                });
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    @Override
    public void close() {}
}