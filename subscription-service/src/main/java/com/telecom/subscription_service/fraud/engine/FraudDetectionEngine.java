package com.telecom.subscription_service.fraud.engine;

import com.telecom.subscription_service.fraud.event.FraudAlertEvent;
import com.telecom.subscription_service.fraud.event.SubscriptionActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complex Event Processing (CEP) engine analyzing stream windows for Telecom Fraud.
 * <p>
 * Evaluates:
 * 1. Geo-Velocity Anomaly: Calculates physical travel speed between consecutive geo-locations.
 * 2. Rapid Action Burst: Detects automated bot / brute-force activity (> 5 actions / 60 seconds).
 */
@Slf4j
@Component
public class FraudDetectionEngine {

    private static final double MAX_REALISTIC_SPEED_KMH = 850.0; // Commercial jet cruising threshold
    private static final int BURST_THRESHOLD = 5;
    private static final long SLIDING_WINDOW_MINUTES = 5;

    // Sliding window buffer: customerId -> List of recent activity events
    private final Map<UUID, List<SubscriptionActivityEvent>> slidingWindows = new ConcurrentHashMap<>();

    public Optional<FraudAlertEvent> processEvent(SubscriptionActivityEvent currentEvent) {
        UUID customerId = currentEvent.customerId();

        List<SubscriptionActivityEvent> customerHistory = slidingWindows.computeIfAbsent(
                customerId, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (customerHistory) {
            LocalDateTime cutoff = currentEvent.timestamp().minusMinutes(SLIDING_WINDOW_MINUTES);
            customerHistory.removeIf(e -> e.timestamp().isBefore(cutoff));

            // Check 1: Geo-Velocity Anomaly against previous action
            if (!customerHistory.isEmpty()) {
                SubscriptionActivityEvent lastEvent = customerHistory.get(customerHistory.size() - 1);
                double distanceKm = calculateHaversineDistance(
                        lastEvent.latitude(), lastEvent.longitude(),
                        currentEvent.latitude(), currentEvent.longitude());

                long secondsBetween = Math.max(1, Duration.between(lastEvent.timestamp(), currentEvent.timestamp()).getSeconds());
                double hoursBetween = secondsBetween / 3600.0;
                double speedKmh = distanceKm / hoursBetween;

                if (distanceKm > 100.0 && speedKmh > MAX_REALISTIC_SPEED_KMH) {
                    log.warn("🚨 [FRAUD CEP] Geo-Velocity anomaly detected for customer {}! Distance: {} km in {}s (Speed: {} km/h)",
                            customerId, String.format("%.1f", distanceKm), secondsBetween, String.format("%.1f", speedKmh));

                    return Optional.of(new FraudAlertEvent(
                            UUID.randomUUID().toString(),
                            currentEvent.subscriptionId(),
                            customerId,
                            "GEO_VELOCITY_ANOMALY",
                            String.format("Impossible spatial movement: %.1f km in %d sec (Speed: %.1f km/h between %s and %s)",
                                    distanceKm, secondsBetween, speedKmh, lastEvent.locationCity(), currentEvent.locationCity()),
                            95,
                            LocalDateTime.now()
                    ));
                }
            }

            customerHistory.add(currentEvent);

            // Check 2: High-frequency Burst Check (e.g. SIM attack / bot spam)
            if (customerHistory.size() >= BURST_THRESHOLD) {
                log.warn("🚨 [FRAUD CEP] Burst rate anomaly detected for customer {}: {} actions within window!",
                        customerId, customerHistory.size());

                return Optional.of(new FraudAlertEvent(
                        UUID.randomUUID().toString(),
                        currentEvent.subscriptionId(),
                        customerId,
                        "RAPID_ACTION_BURST",
                        String.format("Exceeded velocity limit: %d operations within sliding time window", customerHistory.size()),
                        88,
                        LocalDateTime.now()
                ));
            }
        }

        return Optional.empty();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
