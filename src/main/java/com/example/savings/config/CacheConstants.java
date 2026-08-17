package com.example.savings.config;

import java.time.Duration;

/**
 * Cache configuration constants. Centralizes cache names, TTLs, and sizing to prevent
 * inconsistencies between cache setup and cache usage annotations.
 */
public final class CacheConstants {

  private CacheConstants() {
    // Utility class, prevent instantiation
  }

  // Cache names
  public static final String ACCOUNTS_BY_ID = "accountsById";
  public static final String ACCOUNTS_BY_NUMBER = "accountsByNumber";

  // Cache TTL
  public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

  // Connection pool sizing
  public static final int DB_POOL_MAX_SIZE = 10;
  public static final int DB_POOL_MIN_IDLE = 2;

  // Timeouts (milliseconds)
  public static final long CONNECTION_TIMEOUT_MS = 3000;
  public static final long VALIDATION_TIMEOUT_MS = 2000;
  public static final long INITIALIZATION_FAIL_TIMEOUT_MS = -1; // No timeout

  // Database configuration
  public static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/savings";
  public static final String DEFAULT_DB_USER = "savings";
  public static final String DEFAULT_DB_PASSWORD = "savings";

  // Redis configuration
  public static final String DEFAULT_REDIS_HOST = "localhost";
  public static final int DEFAULT_REDIS_PORT = 6379;

  // API paths
  public static final String API_BASE_PATH = "/api/v1";
  public static final String ACCOUNTS_PATH = "/savings-accounts";
  public static final String ACTUATOR_BASE_PATH = API_BASE_PATH + "/actuator";
}
