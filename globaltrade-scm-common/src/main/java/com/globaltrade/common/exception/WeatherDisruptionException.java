package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Supply chain-specific exception for weather-related logistics disruptions.
 *
 * <p><strong>Use Case:</strong> Thrown when severe weather events (hurricanes, blizzards,
 * floods, etc.) disrupt transportation routes, requiring supply chain rerouting and
 * customer communication for delivery expectations.</p>
 *
 * <p>{@code rollback = false}: Weather disruptions are external force majeure events.
 * Transaction rollback would not resolve the weather issue and would lose context
 * needed for customer communication and alternative routing strategies.</p>
 */
@ApplicationException(rollback = false)
public class WeatherDisruptionException extends SupplyChainException {

    private final String weatherEventType;
    private final String affectedRegion;
    private final String affectedRoute;
    private final java.time.LocalDateTime disruptionStartTime;
    private final int estimatedDelayHours;

    public WeatherDisruptionException(String message, String weatherEventType, String affectedRegion,
                                      String affectedRoute, java.time.LocalDateTime disruptionStartTime,
                                      int estimatedDelayHours) {
        super(message, "SCM-WEATHER-001", "WeatherDisruption", affectedRegion);
        this.weatherEventType = weatherEventType;
        this.affectedRegion = affectedRegion;
        this.affectedRoute = affectedRoute;
        this.disruptionStartTime = disruptionStartTime;
        this.estimatedDelayHours = estimatedDelayHours;
    }

    public WeatherDisruptionException(String message, String weatherEventType, String affectedRegion,
                                      String affectedRoute, java.time.LocalDateTime disruptionStartTime,
                                      int estimatedDelayHours, Throwable cause) {
        super(message, "SCM-WEATHER-001", "WeatherDisruption", affectedRegion, cause);
        this.weatherEventType = weatherEventType;
        this.affectedRegion = affectedRegion;
        this.affectedRoute = affectedRoute;
        this.disruptionStartTime = disruptionStartTime;
        this.estimatedDelayHours = estimatedDelayHours;
    }

    public String getWeatherEventType() {
        return weatherEventType;
    }

    public String getAffectedRegion() {
        return affectedRegion;
    }

    public String getAffectedRoute() {
        return affectedRoute;
    }

    public java.time.LocalDateTime getDisruptionStartTime() {
        return disruptionStartTime;
    }

    public int getEstimatedDelayHours() {
        return estimatedDelayHours;
    }
}