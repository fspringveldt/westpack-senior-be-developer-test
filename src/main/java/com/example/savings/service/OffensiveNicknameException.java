package com.example.savings.service;

public class OffensiveNicknameException extends RuntimeException {

  public OffensiveNicknameException() {
    super("Account nick name contains offensive language");
  }
}
