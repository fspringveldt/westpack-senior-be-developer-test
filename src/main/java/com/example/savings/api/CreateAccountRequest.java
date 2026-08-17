package com.example.savings.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new savings account. The API generates both the internal ID and
 * customer-facing account number; clients provide only customer name and an optional nickname.
 * Validation is applied at binding time; trimming handles common whitespace edge cases.
 */
@Schema(description = "Request to create a new savings account")
public record CreateAccountRequest(
    @NotBlank(message = "Customer name is mandatory")
        @Size(max = 100, message = "Customer name must be at most {max} characters")
        @Schema(
            description = "Customer name (required, whitespace trimmed, max 100 characters)",
            example = "Ada Lovelace")
        String customerName,
    @Size(
            min = 5,
            max = 30,
            message = "Account nick name must be between {min} and {max} characters")
        @Schema(
            description =
                "Nickname for the account (optional, 5-30 characters after trimming, filtered for offensive language)",
            example = "Rainy day",
            nullable = true)
        String accountNickName) {

  public CreateAccountRequest {
    customerName = trimToNull(customerName);
    accountNickName = trimToNull(accountNickName);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
