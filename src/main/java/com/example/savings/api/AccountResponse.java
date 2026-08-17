package com.example.savings.api;

import com.example.savings.domain.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a savings account for API responses. Implements Serializable to enable Redis
 * caching with JDK serialization (see RedisCacheConfig for details).
 */
@Schema(description = "A customer's savings account with system-generated identifiers and metadata")
public record AccountResponse(
    @Schema(
            description = "Unique internal identifier for this account",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
    @Schema(
            description = "Customer-facing account number (UUID format)",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String accountNumber,
    @Schema(
            description = "Customer name (required, case-insensitive, max 100 chars)",
            example = "Ada Lovelace")
        String customerName,
    @Schema(
            description =
                "Nickname for the account (optional, 5-30 chars, filtered for offensive content)",
            example = "Rainy day fund",
            nullable = true)
        String accountNickName,
    @Schema(description = "Account creation timestamp (UTC)", example = "2024-01-15T10:30:00Z")
        Instant createdAt)
    implements Serializable {

  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getAccountNumber(),
        account.getCustomerName(),
        account.getAccountNickName(),
        account.getCreatedAt());
  }
}
