package com.example.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.savings.api.AccountResponse;
import com.example.savings.api.CreateAccountRequest;
import com.example.savings.config.AccountPolicyProperties;
import com.example.savings.domain.Account;
import com.example.savings.domain.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final int MAX_ACCOUNTS = 5;

  @Mock private AccountRepository accountRepository;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    AccountPolicyProperties policy =
        new AccountPolicyProperties(MAX_ACCOUNTS, List.of("badword", "wanker"));
    accountService =
        new AccountService(accountRepository, new OffensiveNicknamePolicy(policy), policy);
  }

  @Test
  void createsAccountWithGeneratedIdAndAccountNumber() {
    when(accountRepository.countByCustomerNameIgnoreCase("Ada Lovelace")).thenReturn(0L);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.createAccount(new CreateAccountRequest("Ada Lovelace", "Rainy day"));

    assertThat(response.id()).isNotNull();
    assertThat(response.accountNumber()).isNotBlank();
    assertThat(UUID.fromString(response.accountNumber())).isNotNull();
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.accountNickName()).isEqualTo("Rainy day");
    assertThat(response.createdAt()).isNotNull();
  }

  @Test
  void generatesAUniqueAccountNumberPerAccount() {
    when(accountRepository.countByCustomerNameIgnoreCase(anyString())).thenReturn(0L);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse first =
        accountService.createAccount(new CreateAccountRequest("Ada Lovelace", null));
    AccountResponse second =
        accountService.createAccount(new CreateAccountRequest("Ada Lovelace", null));

    assertThat(first.accountNumber()).isNotEqualTo(second.accountNumber());
    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void allowsAnAbsentNickName() {
    when(accountRepository.countByCustomerNameIgnoreCase("Ada Lovelace")).thenReturn(0L);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AccountResponse response =
        accountService.createAccount(new CreateAccountRequest("Ada Lovelace", null));

    assertThat(response.accountNickName()).isNull();
  }

  @Test
  void persistsTheTrimmedCustomerName() {
    when(accountRepository.countByCustomerNameIgnoreCase("Ada Lovelace")).thenReturn(0L);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    accountService.createAccount(new CreateAccountRequest("  Ada Lovelace  ", "  Rainy day  "));

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository).save(captor.capture());
    assertThat(captor.getValue().getCustomerName()).isEqualTo("Ada Lovelace");
    assertThat(captor.getValue().getAccountNickName()).isEqualTo("Rainy day");
  }

  @Test
  void allowsTheFifthAccount() {
    when(accountRepository.countByCustomerNameIgnoreCase("Ada Lovelace")).thenReturn(4L);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(accountService.createAccount(new CreateAccountRequest("Ada Lovelace", null)))
        .isNotNull();
  }

  @Test
  void rejectsMoreThanFiveAccountsForTheSameCustomer() {
    when(accountRepository.countByCustomerNameIgnoreCase("Ada Lovelace")).thenReturn(5L);

    assertThatThrownBy(
            () -> accountService.createAccount(new CreateAccountRequest("Ada Lovelace", null)))
        .isInstanceOf(AccountLimitExceededException.class)
        .hasMessageContaining("maximum of 5");

    verify(accountRepository, never()).save(any());
  }

  @Test
  void countsExistingAccountsIgnoringCase() {
    when(accountRepository.countByCustomerNameIgnoreCase("ada lovelace")).thenReturn(5L);

    assertThatThrownBy(
            () -> accountService.createAccount(new CreateAccountRequest("ada lovelace", null)))
        .isInstanceOf(AccountLimitExceededException.class);
  }

  @Test
  void rejectsAnOffensiveNickName() {
    assertThatThrownBy(
            () ->
                accountService.createAccount(
                    new CreateAccountRequest("Ada Lovelace", "my badword account")))
        .isInstanceOf(OffensiveNicknameException.class);

    verify(accountRepository, never()).save(any());
  }

  @Test
  void returnsTheAccountById() {
    Account account = Account.open("Ada Lovelace", "Rainy day");
    when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

    AccountResponse response = accountService.getAccount(account.getId());

    assertThat(response.accountNumber()).isEqualTo(account.getAccountNumber());
  }

  @Test
  void throwsWhenTheAccountIdIsUnknown() {
    UUID unknownId = UUID.randomUUID();
    when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.getAccount(unknownId))
        .isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void returnsTheAccountByAccountNumber() {
    Account account = Account.open("Ada Lovelace", null);
    when(accountRepository.findByAccountNumber(account.getAccountNumber()))
        .thenReturn(Optional.of(account));

    assertThat(accountService.getAccountByNumber(account.getAccountNumber()).id())
        .isEqualTo(account.getId());
  }

  @Test
  void throwsWhenTheAccountNumberIsUnknown() {
    when(accountRepository.findByAccountNumber("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.getAccountByNumber("missing"))
        .isInstanceOf(AccountNotFoundException.class);
  }
}
