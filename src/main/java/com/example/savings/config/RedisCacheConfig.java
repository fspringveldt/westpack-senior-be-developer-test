package com.example.savings.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * Configures Redis caching for account lookups (by ID and account number). Cache entries TTL to 10
 * minutes; longer retention is unnecessary given the read-heavy nature of the API. Uses default JDK
 * serialization for speed and reliability in a single-service context. Note: If cross-service
 * caching becomes necessary (e.g., cache inspection, shared cache invalidation), migrate to JSON
 * serialization for interoperability.
 */
@Configuration(proxyBeanMethods = false)
public class RedisCacheConfig implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

  @Bean
  RedisCacheManagerBuilderCustomizer savingsCacheCustomizer() {
    RedisCacheConfiguration configuration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(CacheConstants.DEFAULT_TTL)
            .disableCachingNullValues();
    return builder ->
        builder.withInitialCacheConfigurations(
            Map.of(
                CacheConstants.ACCOUNTS_BY_ID, configuration,
                CacheConstants.ACCOUNTS_BY_NUMBER, configuration));
  }

  /** A Redis outage must not take the API down - log and fall through to the database. */
  @Bean
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Cache read failed for {}[{}], falling back to the database",
            cache.getName(),
            key,
            exception);
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache write failed for {}[{}]", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict failed for {}[{}]", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear failed for {}", cache.getName(), exception);
      }
    };
  }
}
