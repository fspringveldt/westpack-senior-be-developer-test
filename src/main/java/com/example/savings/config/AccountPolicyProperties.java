package com.example.savings.config;

import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized business policy configuration. Separates domain rules from code, enabling product
 * teams to adjust limits and moderation policies without redeployment. Validation ensures the
 * configuration is sensible at startup time.
 */
@Validated
@ConfigurationProperties(prefix = "savings.account")
public record AccountPolicyProperties(
    @Min(1) @DefaultValue("5") int maxAccountsPerCustomer,
    // Offensive nicknames list. In a production system with dynamic moderation,
    // this would be loaded from a database table or external moderation service,
    // with cache invalidation on updates. For this assessment, it's statically
    // configured.
    @DefaultValue({}) List<String> offensiveNicknames) {}
