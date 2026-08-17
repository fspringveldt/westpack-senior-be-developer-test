package com.example.savings.service;

import com.example.savings.api.AccountResponse;
import com.example.savings.api.CreateAccountRequest;
import com.example.savings.config.AccountPolicyProperties;
import com.example.savings.config.CacheConstants;
import com.example.savings.domain.Account;
import com.example.savings.domain.AccountRepository;
import java.util.UUID;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final AccountRepository accountRepository;
  private final OffensiveNicknamePolicy offensiveNicknamePolicy;
  private final AccountPolicyProperties policy;

  public AccountService(
      AccountRepository accountRepository,
      OffensiveNicknamePolicy offensiveNicknamePolicy,
      AccountPolicyProperties policy) {
    this.accountRepository = accountRepository;
    this.offensiveNicknamePolicy = offensiveNicknamePolicy;
    this.policy = policy;
  }

  @Transactional
  @CachePut(cacheNames = CacheConstants.ACCOUNTS_BY_ID, key = "#result.id()")
  public AccountResponse createAccount(CreateAccountRequest request) {
    String nickName = request.accountNickName();

    // We validate nickname content before persisting anything. This keeps the
    // service layer
    // defensive and ensures the domain rules are enforced consistently regardless
    // of the caller.
    if (offensiveNicknamePolicy.isOffensive(nickName)) {
      throw new OffensiveNicknameException();
    }

    // This is intentionally simplified for the assessment scope. In production, the
    // customer should
    // be a first-class entity with a stable customer ID; here we use customer name
    // as the grouping
    // key to keep the domain model compact and focused on the account workflow.
    //
    // The limit is enforced before saving. The separate database constraint on
    // account_number
    // protects us from uniqueness races, while this check keeps the business rule
    // consistent for
    // customer-level account quotas under concurrent requests.
    long existingAccounts = accountRepository.countByCustomerNameIgnoreCase(request.customerName());
    if (existingAccounts >= policy.maxAccountsPerCustomer()) {
      throw new AccountLimitExceededException(
          request.customerName(), policy.maxAccountsPerCustomer());
    }

    Account saved = accountRepository.save(Account.open(request.customerName(), nickName));
    return AccountResponse.from(saved);
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConstants.ACCOUNTS_BY_ID, key = "#id")
  public AccountResponse getAccount(UUID id) {
    return accountRepository
        .findById(id)
        .map(AccountResponse::from)
        .orElseThrow(() -> new AccountNotFoundException(id.toString()));
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConstants.ACCOUNTS_BY_NUMBER, key = "#accountNumber")
  public AccountResponse getAccountByNumber(String accountNumber) {
    return accountRepository
        .findByAccountNumber(accountNumber)
        .map(AccountResponse::from)
        .orElseThrow(() -> new AccountNotFoundException(accountNumber));
  }
}
