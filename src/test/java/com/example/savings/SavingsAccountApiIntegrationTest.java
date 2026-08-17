package com.example.savings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.savings.api.AccountResponse;
import com.example.savings.api.CreateAccountRequest;
import com.example.savings.config.CacheConstants;
import com.example.savings.domain.AccountRepository;
import com.example.savings.service.AccountLimitExceededException;
import com.example.savings.service.AccountService;
import java.time.Duration;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End to end test against a real postgres and redis: verifies the flyway schema, the JPA mapping,
 * the account limit and that GET calls are served from the Redis cache.
 */
@SpringBootTest
@Testcontainers
class SavingsAccountApiIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Autowired private AccountService accountService;

  @MockitoSpyBean private AccountRepository accountRepository;

  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void resetState() {
    accountRepository.deleteAll();
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Test
  void storesAndReadsBackAnAccount() {
    AccountResponse created =
        accountService.createAccount(new CreateAccountRequest("Ada Lovelace", "Rainy day"));

    assertThat(accountRepository.findById(created.id())).isPresent();
    assertThat(accountService.getAccountByNumber(created.accountNumber()).customerName())
        .isEqualTo("Ada Lovelace");
  }

  @Test
  void cachesGetCallsInRedis() {
    AccountResponse created =
        accountService.createAccount(new CreateAccountRequest("Grace Hopper", null));

    Cache cache = cacheManager.getCache(CacheConstants.ACCOUNTS_BY_NUMBER);
    assertThat(cache.get(created.accountNumber())).isNull();

    accountService.getAccountByNumber(created.accountNumber());
    // The cache write is not guaranteed to have reached redis by the time the call
    // returns.
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(cache.get(created.accountNumber(), AccountResponse.class))
                    .isEqualTo(created));

    clearInvocations(accountRepository);
    assertThat(accountService.getAccountByNumber(created.accountNumber())).isEqualTo(created);
    // The second read never reached postgres.
    verify(accountRepository, never()).findByAccountNumber(created.accountNumber());
  }

  @Test
  void enforcesTheFiveAccountLimitAgainstTheDatabase() {
    IntStream.range(0, 5)
        .forEach(
            index -> accountService.createAccount(new CreateAccountRequest("Alan Turing", null)));

    assertThat(accountRepository.countByCustomerNameIgnoreCase("alan turing")).isEqualTo(5);
    assertThatThrownBy(
            () -> accountService.createAccount(new CreateAccountRequest("alan turing", null)))
        .isInstanceOf(AccountLimitExceededException.class);
  }

  @Test
  void allowsMultipleAccountsPerCustomerUpToTheLimit() {
    // Customers should be able to create multiple accounts (up to 5)
    IntStream.range(0, 5)
        .forEach(
            index ->
                accountService.createAccount(
                    new CreateAccountRequest("Ada Lovelace", "Account " + index)));

    assertThat(accountRepository.countByCustomerNameIgnoreCase("ada lovelace")).isEqualTo(5);

    // Only the 6th should be rejected
    assertThatThrownBy(
            () ->
                accountService.createAccount(
                    new CreateAccountRequest("Ada Lovelace", "Sixth account")))
        .isInstanceOf(AccountLimitExceededException.class);
  }
}
