package com.example.savings.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

  long countByCustomerNameIgnoreCase(String customerName);

  Optional<Account> findByAccountNumber(String accountNumber);
}
