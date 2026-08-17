package com.example.savings.service;

/** Raised when a customer already holds the maximum number of savings accounts. */
public class AccountLimitExceededException extends RuntimeException {

  private final int limit;

  public AccountLimitExceededException(String customerName, int limit) {
    super(
        "Customer '%s' already holds the maximum of %d savings accounts"
            .formatted(customerName, limit));
    this.limit = limit;
  }

  public int getLimit() {
    return limit;
  }
}
