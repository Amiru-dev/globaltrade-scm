package com.globaltrade.common.util;

/**
 * System-wide constants for the GlobalTrade SCM application.
 *
 * <p><strong>Critical Analysis — Constants Extraction:</strong></p>
 * <ul>
 *   <li><strong>Maintainability:</strong> Centralized constants eliminate magic numbers
 *       and enable single-point-of-change for configuration values.</li>
 *   <li><strong>Consistency:</strong> Ensures consistent behavior across components
 *       by using the same constant values throughout the application.</li>
 *   <li><strong>Documentation:</strong> Constants serve as self-documenting code,
 *       making the purpose of numeric values clear.</li>
 *   <li><strong>Testability:</strong> Constants can be easily modified for testing
 *       scenarios without changing business logic.</li>
 * </ul>
 */
public final class SystemConstants {

    // ======================== Security Constants ========================

    /**
     * Maximum number of failed login attempts before account lockout.
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * Account lockout duration in minutes after failed login attempts.
     */
    public static final int ACCOUNT_LOCK_MINUTES = 30;

    /**
     * High-value transaction threshold for additional authorization.
     */
    public static final int HIGH_VALUE_THRESHOLD = 100000;

    /**
     * BCrypt work factor for password hashing.
     */
    public static final int BCRYPT_ROUNDS = 12;

    /**
     * Maximum login requests per client IP within the rate limit window.
     */
    public static final int RATE_LIMIT_LOGIN_MAX_REQUESTS = 10;

    /**
     * Maximum refresh token requests per client IP within the rate limit window.
     */
    public static final int RATE_LIMIT_REFRESH_MAX_REQUESTS = 20;

    /**
     * Rate limit sliding window duration in seconds.
     */
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    // ======================== Transaction Constants ========================

    /**
     * Default transaction timeout in seconds.
     */
    public static final int DEFAULT_TRANSACTION_TIMEOUT_SECONDS = 300;

    /**
     * Default transaction timeout in minutes.
     */
    public static final int DEFAULT_TRANSACTION_TIMEOUT_MINUTES = 5;

    // ======================== Timer Constants ========================

    /**
     * Shipment tracking interval in minutes.
     */
    public static final int SHIPMENT_TRACKING_INTERVAL_MINUTES = 15;

    /**
     * Inventory monitoring interval in hours.
     */
    public static final int INVENTORY_MONITOR_INTERVAL_HOURS = 1;

    /**
     * Vendor performance check interval in hours.
     */
    public static final int VENDOR_PERFORMANCE_INTERVAL_HOURS = 24;

    /**
     * Refresh token cleanup hour (2 AM).
     */
    public static final int REFRESH_TOKEN_CLEANUP_HOUR = 2;

    // ======================== JWT Token Constants ========================

    /**
     * JWT access token expiry time in milliseconds (15 minutes).
     */
    public static final long JWT_ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000;

    /**
     * JWT access token expiry time in minutes.
     */
    public static final int JWT_ACCESS_TOKEN_EXPIRY_MINUTES = 15;

    /**
     * JWT refresh token expiry time in days.
     */
    public static final int JWT_REFRESH_TOKEN_EXPIRY_DAYS = 30;

    // ======================== Inventory Constants ========================

    /**
     * Default reorder level for inventory items.
     */
    public static final int DEFAULT_REORDER_LEVEL = 10;

    /**
     * Default maximum stock level for inventory items.
     */
    public static final int DEFAULT_MAX_STOCK_LEVEL = 1000;

    /**
     * Low stock threshold percentage (below reorder level).
     */
    public static final double LOW_STOCK_THRESHOLD_PERCENTAGE = 0.2;

    // ======================== Session Constants ========================

    /**
     * Stateful bean session timeout in minutes.
     */
    public static final int STATEFUL_SESSION_TIMEOUT_MINUTES = 30;

    /**
     * HTTP session timeout in minutes.
     */
    public static final int HTTP_SESSION_TIMEOUT_MINUTES = 30;

    // ======================== Pagination Constants ========================

    /**
     * Default page size for paginated results.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size for paginated results.
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ======================== Performance Constants ========================

    /**
     * Performance warning threshold in milliseconds.
     */
    public static final long PERFORMANCE_WARN_THRESHOLD_MS = 1000;

    /**
     * Performance severe threshold in milliseconds.
     */
    public static final long PERFORMANCE_SEVERE_THRESHOLD_MS = 5000;

    // ======================== Customs Constants ========================

    /**
     * Customs submission deadline warning days.
     */
    public static final int CUSTOMS_DEADLINE_WARNING_DAYS = 3;

    /**
     * Default customs processing days estimate.
     */
    public static final int DEFAULT_CUSTOMS_PROCESSING_DAYS = 5;

    // ======================== Vendor Constants ========================

    /**
     * Minimum vendor performance rating.
     */
    public static final double MIN_VENDOR_RATING = 0.0;

    /**
     * Maximum vendor performance rating.
     */
    public static final double MAX_VENDOR_RATING = 5.0;

    /**
     * Vendor performance review interval in days.
     */
    public static final int VENDOR_PERFORMANCE_REVIEW_DAYS = 30;

    // ======================== Shipment Constants ========================

    /**
     * Shipment tracking update interval in hours.
     */
    public static final int SHIPMENT_TRACKING_UPDATE_HOURS = 4;

    /**
     * Delayed shipment threshold in days.
     */
    public static final int DELAYED_SHIPMENT_THRESHOLD_DAYS = 2;

    // ======================== Database Constants ========================

    /**
     * Database connection pool initial size.
     */
    public static final int DB_POOL_INITIAL_SIZE = 5;

    /**
     * Database connection pool maximum size.
     */
    public static final int DB_POOL_MAX_SIZE = 20;

    /**
     * Database connection pool minimum idle size.
     */
    public static final int DB_POOL_MIN_IDLE = 2;

    // ======================== Private Constructor ========================

    /**
     * Private constructor to prevent instantiation.
     */
    private SystemConstants() {
        throw new AssertionError("SystemConstants class should not be instantiated");
    }
}