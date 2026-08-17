package com.example.savings.service;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String identifier) {
    super("No savings account found for '%s'".formatted(identifier));
  }
}
