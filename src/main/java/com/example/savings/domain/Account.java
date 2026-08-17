package com.example.savings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Core domain model for a savings account.
 *
 * <p>The design separates the internal technical ID from the customer-facing account number. This
 * is intentional: it keeps public identifiers stable and avoids leaking or depending on database
 * surrogate keys in external API contracts.
 *
 * <p>The project is intentionally simple for the interview scope, but the separation of concerns
 * here is the right foundation for growth. A real production system would likely evolve to a
 * dedicated Customer entity and a stronger identity model, but this keeps the domain easier to
 * reason about while still enforcing strong constraints at the persistence layer.
 */
@Entity
@Table(
    name = "account",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_account_account_number", columnNames = "account_number"),
    indexes = @Index(name = "idx_account_customer_name", columnList = "customer_name"))
public class Account {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "account_number", nullable = false, updatable = false, length = 36)
  private String accountNumber;

  @Column(name = "customer_name", nullable = false, length = 100)
  private String customerName;

  @Column(name = "account_nick_name", length = 30)
  private String accountNickName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Account() {
    // Required by JPA. The entity is intentionally simple and uses a factory method
    // for creation
    // so the object graph is constructed intentionally rather than mutated after
    // creation.
  }

  private Account(
      UUID id,
      String accountNumber,
      String customerName,
      String accountNickName,
      Instant createdAt) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.customerName = customerName;
    this.accountNickName = accountNickName;
    this.createdAt = createdAt;
  }

  /**
   * Factory method for creating accounts with system-generated identifiers.
   *
   * <p>Both the internal ID and the customer-facing account number are generated at creation time.
   * This removes predictability from the API contract and keeps database identity separate from the
   * business-facing identifier.
   */
  public static Account open(String customerName, String accountNickName) {
    return new Account(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        customerName,
        accountNickName,
        Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getAccountNickName() {
    return accountNickName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Account account && id != null && id.equals(account.id);
  }

  @Override
  public int hashCode() {
    return Account.class.hashCode();
  }
}
